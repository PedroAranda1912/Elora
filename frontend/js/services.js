let servicePage = 0;
let serviceLoading = false;
let serviceSearchTimer;
let serviceCategories = [];

document.addEventListener('DOMContentLoaded', async () => {
    const query = new URLSearchParams(location.search);
    const dialog = document.getElementById('service-filter-dialog');
    const mobile = window.matchMedia('(max-width: 760px)');
    if (mobile.matches && dialog.open) dialog.close();
    document.getElementById('open-service-filters').addEventListener('click', () => { if (!dialog.open) dialog.showModal(); });
    document.getElementById('close-service-filters').addEventListener('click', () => dialog.close());
    document.getElementById('apply-service-filters').addEventListener('click', () => { dialog.close(); loadServices(true); });
    mobile.addEventListener('change', event => {
        if (event.matches && dialog.open) dialog.close();
        else if (!event.matches && !dialog.open) dialog.show();
    });

    const category = document.getElementById('service-category');
    serviceCategories = await apiCall('/categories?type=SERVICE').catch(() => ['Programação', 'Design', 'Marketing', 'Social Media', 'Edição de vídeo', 'Fotografia', 'Redação', 'Tradução', 'Música', 'Aulas', 'Consultoria', 'Arquitetura', '3D', 'Finanças', 'Outros']);
    serviceCategories.forEach(item => category.add(new Option(item, item)));
    const chipRoot = document.getElementById('service-category-chips');
    chipRoot.innerHTML = `<button class="service-category-chip is-active" type="button" data-category="">Todos</button>${serviceCategories.map(item => `<button class="service-category-chip" type="button" data-category="${escapeHtml(item)}">${escapeHtml(item)}</button>`).join('')}`;
    chipRoot.addEventListener('click', event => {
        const chip = event.target.closest('[data-category]'); if (!chip) return;
        category.value = chip.dataset.category; updateCategoryChips(); loadServices(true);
    });
    category.value = query.get('category') || '';
    document.getElementById('service-search').value = query.get('q') || '';
    document.getElementById('service-search-form').addEventListener('submit', event => { event.preventDefault(); loadServices(true); });
    document.querySelectorAll('[data-service-query]').forEach(button => button.addEventListener('click', () => { document.getElementById('service-search').value = button.dataset.serviceQuery; loadServices(true); }));
    document.querySelectorAll('#service-search, #service-location, #service-min-price, #service-max-price').forEach(input => input.addEventListener('input', () => { if (mobile.matches && input.id !== 'service-search') updateActiveFilterCount(); else scheduleServiceSearch(); }));
    document.querySelectorAll('#service-category, #service-max-days, #service-min-rating, #service-experience, #service-mode, #service-sort').forEach(input => input.addEventListener('change', () => { updateCategoryChips(); updateActiveFilterCount(); if (!mobile.matches) loadServices(true); }));
    document.getElementById('clear-service-filters').addEventListener('click', () => {
        ['service-search', 'service-location', 'service-min-price', 'service-max-price', 'service-max-days', 'service-min-rating', 'service-experience', 'service-mode'].forEach(id => { document.getElementById(id).value = ''; });
        category.value = ''; document.getElementById('service-sort').value = 'relevance';
        history.replaceState(null, '', 'servicos.html'); updateCategoryChips(); updateActiveFilterCount(); loadServices(true);
    });
    document.getElementById('load-more-services').addEventListener('click', () => loadServices(false));
    updateCategoryChips(); updateActiveFilterCount(); loadServices(true);
});

function scheduleServiceSearch() { clearTimeout(serviceSearchTimer); serviceSearchTimer = setTimeout(() => loadServices(true), 280); }
function updateCategoryChips() {
    const selected = document.getElementById('service-category').value;
    document.querySelectorAll('.service-category-chip').forEach(chip => chip.classList.toggle('is-active', chip.dataset.category === selected));
}
function updateActiveFilterCount() {
    const ids = ['service-category', 'service-min-price', 'service-max-price', 'service-max-days', 'service-min-rating', 'service-experience', 'service-location', 'service-mode'];
    const count = ids.filter(id => document.getElementById(id).value).length;
    const output = document.getElementById('active-service-filter-count');
    output.hidden = !count; output.textContent = String(count);
}

async function loadServices(reset) {
    if (serviceLoading) return;
    serviceLoading = true;
    const list = document.getElementById('service-list'); const button = document.getElementById('load-more-services');
    if (reset) { servicePage = 0; list.innerHTML = '<div class="skeleton-card" aria-hidden="true"><span></span><span></span><span></span></div><div class="skeleton-card" aria-hidden="true"><span></span><span></span><span></span></div><div class="skeleton-card" aria-hidden="true"><span></span><span></span><span></span></div><div class="skeleton-card" aria-hidden="true"><span></span><span></span><span></span></div>'; }
    button.disabled = true;
    const params = new URLSearchParams({ page: String(servicePage), size: '24', sort: document.getElementById('service-sort').value });
    const filters = { q: document.getElementById('service-search').value.trim(), category: document.getElementById('service-category').value,
        minPrice: document.getElementById('service-min-price').value, maxPrice: document.getElementById('service-max-price').value,
        location: document.getElementById('service-location').value.trim(), maxDays: document.getElementById('service-max-days').value,
        minRating: document.getElementById('service-min-rating').value, experience: document.getElementById('service-experience').value,
        mode: document.getElementById('service-mode').value };
    Object.entries(filters).forEach(([key, value]) => { if (value) params.set(key, value); });
    try {
        const page = await apiCall(`/services/search?${params}`);
        const cards = page.content.map(service => serviceCard(service)).join('');
        if (reset) list.innerHTML = cards || '<div class="empty-state card-empty"><h2>Nenhum serviço encontrado</h2><p>Tente remover um filtro ou buscar por outra habilidade.</p><button class="btn btn-secondary" type="button" onclick="document.getElementById(\'clear-service-filters\').click()">Limpar filtros</button></div>';
        else if (cards) list.insertAdjacentHTML('beforeend', cards);
        servicePage = page.page + 1; button.hidden = !page.hasNext; button.disabled = false;
        document.getElementById('service-results').textContent = `${page.totalElements} ${page.totalElements === 1 ? 'profissional encontrado' : 'profissionais encontrados'}`;
        const url = new URL(location.href); ['q', 'category'].forEach(key => filters[key] ? url.searchParams.set(key, filters[key]) : url.searchParams.delete(key));
        history.replaceState(null, '', url.pathname + url.search); syncFavoriteButtons();
    } catch (error) {
        if (reset) list.innerHTML = `<div class="empty-state card-empty"><h2>Não foi possível carregar os serviços</h2><p>${escapeHtml(error.message || 'Confira sua conexão e tente novamente.')}</p><button class="btn btn-secondary" type="button" onclick="loadServices(true)">Tentar novamente</button></div>`;
        button.hidden = true; document.getElementById('service-results').textContent = 'Catálogo indisponível';
    } finally { serviceLoading = false; }
}

