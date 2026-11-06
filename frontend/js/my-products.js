let myProducts = [];
let pendingDeleteId = null;

document.addEventListener('DOMContentLoaded', () => {
    if (!requireAuth()) return;
    document.getElementById('mine-category').addEventListener('change', renderMyProducts);
    document.getElementById('mine-status').addEventListener('change', renderMyProducts);
    document.getElementById('confirm-delete-product').addEventListener('click', deleteProduct);
    loadMyProducts();
    loadProductCategories();
});

async function loadProductCategories() {
    const select = document.getElementById('mine-category');
    const categories = await apiCall('/categories?type=PRODUCT').catch(() => myProducts.map(product => product.category).filter(Boolean));
    addProductCategoryOptions(categories);
}

function addProductCategoryOptions(categories) {
    const select = document.getElementById('mine-category');
    const existing = new Set([...select.options].map(option => option.value));
    [...new Set(categories)].sort((a, b) => a.localeCompare(b, 'pt-BR')).forEach(category => {
        if (category && !existing.has(category)) select.add(new Option(category, category));
    });
}

async function loadMyProducts() {
    try { myProducts = await apiCall('/products/mine'); addProductCategoryOptions(myProducts.map(product => product.category)); renderMyProducts(); }
    catch (error) { document.getElementById('product-list').innerHTML = `<p class="empty-state">${escapeHtml(error.message || 'Não foi possível carregar seus anúncios.')}</p>`; }
}

function renderMyProducts() {
    const category = document.getElementById('mine-category').value;
    const status = document.getElementById('mine-status').value;
    const visible = myProducts.filter(p => p.status !== 'REMOVIDO'
        && (!category || p.category === category)
        && (status === 'ALL' || (status === 'LOW_STOCK'
            ? p.status === 'ATIVO' && Number(p.stock) > 0 && Number(p.stock) <= 3
            : p.status === status)));
    const list = document.getElementById('product-list');
    list.innerHTML = visible.length ? visible.map(myProductCard).join('') : '<div class="empty-state card-empty"><h2>Nenhum anúncio encontrado</h2><p>Quando publicar produtos, você poderá gerenciar tudo aqui.</p><a href="vender.html" class="btn">Publicar produto</a></div>';
    list.querySelectorAll('[data-product-status]').forEach(button => button.addEventListener('click', () => changeProductStatus(button)));
    list.querySelectorAll('[data-product-delete]').forEach(button => button.addEventListener('click', () => {
        pendingDeleteId = Number(button.dataset.productDelete);
        const product = myProducts.find(item => item.id === pendingDeleteId);
        document.getElementById('delete-product-copy').textContent = `“${product?.name || 'Este anúncio'}” será removido da sua lista. Pedidos históricos continuarão preservados.`;
        document.getElementById('delete-product-dialog').showModal();
    }));
}

function myProductCard(p) {
    const active = p.status === 'ATIVO';
    return `<article class="card seller-product-card"><a class="listing-image" href="${active ? `produto.html?id=${p.id}` : `editar-produto.html?id=${p.id}`}" aria-label="${active ? 'Ver anúncio' : 'Editar anúncio'}"><img src="${escapeHtml(p.imageUrl || FALLBACK_IMG)}" alt="${escapeHtml(p.name)}" loading="lazy" onerror="this.onerror=null;this.src='${FALLBACK_IMG}'"></a><div class="listing-card-copy"><div class="listing-meta"><span class="badge ${active ? 'badge-ativo' : 'badge-pausado'}">${active ? 'Ativo' : 'Pausado'}</span><span class="badge badge-pausado">${Number(p.stock) > 0 ? `${p.stock} em estoque` : 'Sem estoque'}</span></div><h2>${escapeHtml(p.name)}</h2><p class="muted-copy">${escapeHtml(p.category || 'Sem categoria')}</p><strong class="price">${formatCurrency(p.price)}</strong><div class="seller-card-actions"><a class="btn btn-secondary" href="editar-produto.html?id=${p.id}">Editar</a>${active ? `<button class="btn btn-secondary" data-product-status="${p.id}:pause">Pausar</button>` : `<button class="btn btn-secondary" data-product-status="${p.id}:activate">Ativar</button>`}${active ? `<a class="btn btn-secondary" href="produto.html?id=${p.id}">Ver anúncio</a>` : ''}<button class="btn btn-secondary" type="button" data-product-delete="${p.id}">Remover</button></div></div></article>`;
}

async function changeProductStatus(button) {
    const [id, action] = button.dataset.productStatus.split(':'); setButtonLoading(button, true, 'Atualizando…');
    try { await apiCall(`/products/${id}/${action}`, 'PUT'); showToast(action === 'pause' ? 'Anúncio pausado.' : 'Anúncio ativado.'); await loadMyProducts(); }
    catch (error) { showToast(error.message || 'Não foi possível atualizar o anúncio.', 'error'); setButtonLoading(button, false); }
}

async function deleteProduct() {
    if (!pendingDeleteId) return;
    const button = document.getElementById('confirm-delete-product'); setButtonLoading(button, true, 'Removendo…');
    try {
        await apiCall(`/products/${pendingDeleteId}`, 'DELETE');
        myProducts = myProducts.filter(product => product.id !== pendingDeleteId);
        document.getElementById('delete-product-dialog').close(); renderMyProducts(); showToast('Anúncio removido.');
    } catch (error) { showToast(error.message || 'Não foi possível remover o anúncio.', 'error'); }
    finally { pendingDeleteId = null; setButtonLoading(button, false); }
}
