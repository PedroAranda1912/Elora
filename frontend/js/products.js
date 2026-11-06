let productPage = 0;
let productHasNext = false;
let productSearchTimer;
let productLoading = false;

document.addEventListener('DOMContentLoaded', async () => {
    const filterDialog = document.getElementById('product-filter-dialog');
    const openFilters = document.getElementById('open-product-filters');
    const mobileFilters = window.matchMedia('(max-width: 768px)');
    const syncFiltersDialog = () => {
        if (mobileFilters.matches) {
            if (filterDialog.open && !filterDialog.matches(':modal')) filterDialog.close();
            openFilters.hidden = false;
        } else {
            if (filterDialog.matches(':modal')) filterDialog.close();
            if (!filterDialog.open) filterDialog.show();
            openFilters.hidden = true;
        }
    };
    syncFiltersDialog();
    mobileFilters.addEventListener('change', syncFiltersDialog);
    openFilters.addEventListener('click', () => filterDialog.showModal());
    document.getElementById('close-product-filters').addEventListener('click', () => filterDialog.close());
    document.getElementById('apply-product-filters').addEventListener('click', () => filterDialog.close());
    filterDialog.addEventListener('click', event => { if (event.target === filterDialog && filterDialog.matches(':modal')) filterDialog.close(); });

    const params = new URLSearchParams(location.search);
    document.getElementById('product-search').value = params.get('q') || '';
    ['category', 'product-category'].forEach(() => {});
    const categories = await apiCall('/categories?type=PRODUCT').catch(() => []);
    const select = document.getElementById('product-category');
    categories.forEach(category => select.add(new Option(category, category)));
    select.value = params.get('category') || '';
    document.querySelectorAll('#product-search, #product-location, #product-min-price, #product-max-price').forEach(input => input.addEventListener('input', scheduleProductSearch));
    document.querySelectorAll('#product-category, #product-condition, #product-sort, #product-in-stock').forEach(input => input.addEventListener('change', () => loadProducts(true)));
    document.getElementById('clear-product-filters').addEventListener('click', () => {
        document.getElementById('product-search').value = ''; select.value = '';
        document.getElementById('product-condition').value = ''; document.getElementById('product-min-price').value = '';
        document.getElementById('product-max-price').value = ''; document.getElementById('product-location').value = '';
        document.getElementById('product-sort').value = 'recent'; document.getElementById('product-in-stock').checked = false;
        history.replaceState(null, '', 'produtos.html'); loadProducts(true);
    });
    document.getElementById('load-more-products').addEventListener('click', () => loadProducts(false));
    document.getElementById('product-list').addEventListener('click', async event => {
        const compareButton = event.target.closest('[data-compare-product]');
        if (compareButton) { toggleProductComparison(Number(compareButton.dataset.compareProduct)); return; }
        const button = event.target.closest('[data-add-product]'); if (!button) return;
        if (!requireAuth()) return;
        setButtonLoading(button, true, 'Adicionando…');
        try {
            const product = await apiCall(`/products/${button.dataset.addProduct}`);
            const profile = await apiCall('/users/me');
            if (String(profile.id) === String(product.owner?.id)) {
                showCartToast('Você não pode comprar seu próprio produto.', 'error');
                return;
            }
            addToCart(product, 1);
        }
        catch (error) { showToast(error.message || 'Não foi possível adicionar este produto.', 'error'); }
        finally { setButtonLoading(button, false); }
    });
    loadProducts(true);
});

function scheduleProductSearch() { clearTimeout(productSearchTimer); productSearchTimer = setTimeout(() => loadProducts(true), 240); }

async function loadProducts(reset) {
    if (productLoading) return;
    productLoading = true;
    const list = document.getElementById('product-list');
    const button = document.getElementById('load-more-products');
    const results = document.getElementById('product-results');
    if (reset) { productPage = 0; list.innerHTML = '<div class="skeleton-card" aria-hidden="true"><span></span><span></span><span></span></div><div class="skeleton-card" aria-hidden="true"><span></span><span></span><span></span></div><div class="skeleton-card" aria-hidden="true"><span></span><span></span><span></span></div><div class="skeleton-card" aria-hidden="true"><span></span><span></span><span></span></div>'; }
    button.disabled = true;
    const params = new URLSearchParams({ page: String(productPage), size: '24', sort: document.getElementById('product-sort').value });
    const values = {
        q: document.getElementById('product-search').value.trim(), category: document.getElementById('product-category').value,
        condition: document.getElementById('product-condition').value, minPrice: document.getElementById('product-min-price').value,
        maxPrice: document.getElementById('product-max-price').value, location: document.getElementById('product-location').value.trim()
    };
    Object.entries(values).forEach(([key, value]) => { if (value) params.set(key, value); });
    if (document.getElementById('product-in-stock').checked) params.set('inStock', 'true');
    try {
        const page = await apiCall(`/products/search?${params}`);
        const cards = page.content.map(product => productCard(product)).join('');
        if (reset) list.innerHTML = cards || '<div class="empty-state card-empty"><h2>Nenhum produto encontrado</h2><p>Tente mudar a busca ou remover algum filtro.</p><button class="btn btn-secondary" type="button" onclick="clearProductFilters()">Limpar filtros</button></div>';
        else if (cards) list.insertAdjacentHTML('beforeend', cards);
        productHasNext = page.hasNext;
        button.hidden = !productHasNext; button.disabled = false;
        results.textContent = `${page.totalElements} ${page.totalElements === 1 ? 'produto encontrado' : 'produtos encontrados'}`;
        syncFavoriteButtons();
        refreshProductComparison();
    } catch (error) {
        if (reset) list.innerHTML = '<div class="empty-state card-empty"><h2>Não foi possível carregar os anúncios</h2><p>Confira sua conexão e tente novamente em instantes.</p><button class="btn btn-secondary" type="button" onclick="loadProducts(true)">Tentar novamente</button></div>';
        button.hidden = true; results.textContent = 'Catálogo indisponível';
    } finally { productLoading = false; }
}


function clearProductFilters() {
    document.getElementById('clear-product-filters').click();
}
