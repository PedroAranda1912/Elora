let homeKind = 'PRODUCT';
let homeKindTransitionTimer;
let homeSearchPulseTimer;

document.addEventListener('DOMContentLoaded', () => {
    const year = document.getElementById('footer-year');
    if (year) year.textContent = String(new Date().getFullYear());

    document.querySelectorAll('[data-home-kind]').forEach(button => button.addEventListener('click', () => {
        const nextKind = button.dataset.homeKind;
        const changed = nextKind !== homeKind;
        homeKind = nextKind;
        document.querySelectorAll('[data-home-kind]').forEach(item => {
            const active = item === button;
            item.classList.toggle('is-active', active);
            item.setAttribute('aria-pressed', String(active));
        });
        const field = document.getElementById('home-query');
        const placeholder = homeKind === 'PRODUCT' ? 'O que voc\u00ea est\u00e1 procurando?' : 'Qual talento voc\u00ea precisa?';
        const search = document.getElementById('home-search');
        if (changed && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            window.clearTimeout(homeKindTransitionTimer);
            window.clearTimeout(homeSearchPulseTimer);
            field.classList.add('is-placeholder-changing');
            search.classList.add('is-switching');
            homeKindTransitionTimer = window.setTimeout(() => {
                field.placeholder = placeholder;
                requestAnimationFrame(() => field.classList.remove('is-placeholder-changing'));
            }, 90);
            homeSearchPulseTimer = window.setTimeout(() => search.classList.remove('is-switching'), 320);
        } else {
            field.placeholder = placeholder;
        }
        field.setAttribute('aria-label', homeKind === 'PRODUCT' ? 'Buscar produtos' : 'Buscar servi\u00e7os e profissionais');
        field.focus();
    }));

    document.getElementById('home-search').addEventListener('submit', event => {
        event.preventDefault();
        const query = document.getElementById('home-query').value.trim();
        const destination = homeKind === 'PRODUCT' ? 'produtos.html' : 'servicos.html';
        location.href = `${destination}${query ? `?q=${encodeURIComponent(query)}` : ''}`;
    });

    loadHomeCategories();
    loadHomeListings();
});

// As seções ficam ocultas até haver anúncios; com o banco vazio, a home continua só com busca e categorias.
async function loadHomeListings() {
    const [products, services] = await Promise.allSettled([
        apiCall('/products/search?size=8&sort=recent'),
        apiCall('/services/search?size=24&sort=sales')
    ]);
    const productItems = products.status === 'fulfilled' ? products.value.content || [] : [];
    // Um serviço por prestador, priorizando quem tem mais trabalhos concluídos (ordem que a API já devolve).
    const seenProviders = new Set();
    const serviceItems = (services.status === 'fulfilled' ? services.value.content || [] : [])
        .filter(service => {
            const ownerId = service.owner?.id;
            if (seenProviders.has(ownerId)) return false;
            seenProviders.add(ownerId);
            return true;
        })
        .slice(0, 4);
    renderHomeListing('home-latest-products', productItems, item => productCard(item, { actions: false }));
    renderHomeListing('home-latest-services', serviceItems, serviceCard);
    if (productItems.length || serviceItems.length) syncFavoriteButtons();
}

function renderHomeListing(gridId, items, card) {
    const grid = document.getElementById(gridId);
    const section = grid?.closest('section');
    if (!grid || !section || !items.length) return;
    grid.innerHTML = items.map(item => card(item)).join('');
    section.hidden = false;
}

async function loadHomeCategories() {
    const productBox = document.getElementById('home-product-categories');
    const serviceBox = document.getElementById('home-service-categories');
    const [products, services] = await Promise.allSettled([
        apiCall('/categories?type=PRODUCT'),
        apiCall('/categories?type=SERVICE')
    ]);
    renderCategoryLinks(productBox, products, 'produtos.html', 'Não há categorias de produtos para mostrar ainda.');
    renderCategoryLinks(serviceBox, services, 'servicos.html', 'Não há categorias de serviços para mostrar ainda.');
}

function renderCategoryLinks(container, result, path, emptyMessage) {
    if (!container) return;
    if (result.status !== 'fulfilled') {
        container.innerHTML = '<span class="category-empty">Não foi possível carregar as categorias agora.</span>';
        return;
    }
    if (!result.value.length) {
        container.innerHTML = `<span class="category-empty">${escapeHtml(emptyMessage)}</span>`;
        return;
    }
    container.innerHTML = result.value.slice(0, 6).map(category =>
        `<a class="category-chip" href="${path}?category=${encodeURIComponent(category)}">${escapeHtml(category)}<span aria-hidden="true">↗</span></a>`
    ).join('');
}
