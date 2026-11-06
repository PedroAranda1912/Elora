function renderNav() {
    const nav = document.getElementById('nav-menu');
    if (!nav) return;
    const header = nav.closest('header');
    header?.classList.add('site-header');
    nav.classList.add('site-nav');

    if (!document.getElementById('nav-toggle') && header) {
        const toggle = document.createElement('button');
        toggle.id = 'nav-toggle';
        toggle.className = 'nav-toggle';
        toggle.type = 'button';
        toggle.setAttribute('aria-controls', 'nav-menu');
        toggle.setAttribute('aria-expanded', 'false');
        toggle.setAttribute('aria-label', 'Abrir menu');
        toggle.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16M4 12h16M4 17h16"/></svg>';
        header.insertBefore(toggle, nav);
        toggle.addEventListener('click', () => {
            const open = header.classList.toggle('nav-open');
            toggle.setAttribute('aria-expanded', String(open));
            toggle.setAttribute('aria-label', open ? 'Fechar menu' : 'Abrir menu');
        });
        document.addEventListener('keydown', event => {
            if (event.key === 'Escape') {
                header.classList.remove('nav-open');
                toggle.setAttribute('aria-expanded', 'false');
            }
        });
    }

    // Hierarquia: navegar (Produtos, Serviços, Projetos) | anunciar | atalhos pessoais em ícones | conta.
    // O menu da conta agrupa as áreas pessoais pelos mesmos dois lados do marketplace, sem repetir a barra.
    const authenticated = Boolean(getToken());
    const storedName = (localStorage.getItem('userName') || '').trim();
    const fullName = storedName || 'Minha conta';
    const firstName = escapeHtml(storedName ? storedName.split(/\s+/)[0] : 'Conta');
    const name = escapeHtml(fullName);
    const initial = name.charAt(0).toUpperCase();
    const icon = path => `<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">${path}</svg>`;
    const iconLink = (href, label, path, extra = '') =>
        `<a class="nav-icon-link" href="${href}" title="${label}">${icon(path)}<span class="nav-icon-label">${label}</span>${extra}</a>`;

    const personal = authenticated ? `
        <a class="nav-sell" href="vender.html" title="Anunciar produto ou serviço">${icon('<path d="M12 5v14M5 12h14"/>')}<span class="nav-sell-label">Anunciar</span></a>
        ${iconLink('mensagens.html', 'Mensagens', '<path d="M20 11.5a7.5 7.5 0 0 1-7.5 7.5H4l2.1-3.2A7.5 7.5 0 1 1 20 11.5Z"/>')}
        ${iconLink('notificacoes.html', 'Notificações', '<path d="M6 9.5a6 6 0 1 1 12 0c0 5.5 2.5 7 2.5 7h-17S6 15 6 9.5Z"/><path d="M10 20a2.2 2.2 0 0 0 4 0"/>',
            '<span class="nav-notification-count" id="nav-notification-count" hidden>0</span>')}
        ${iconLink('favoritos.html', 'Favoritos', '<path d="M20.8 8.8c0 5.2-8.8 11-8.8 11s-8.8-5.8-8.8-11a4.7 4.7 0 0 1 8.8-2.2 4.7 4.7 0 0 1 8.8 2.2Z"/>')}` : '';

    const account = authenticated ? `
        <details class="nav-account">
            <summary title="${name}"><span class="nav-avatar" aria-hidden="true">${initial}</span><span class="nav-account-name">${firstName}</span><svg viewBox="0 0 16 16" aria-hidden="true"><path d="m4 6 4 4 4-4"/></svg></summary>
            <div class="nav-account-menu">
                <div class="nav-account-head"><span class="nav-avatar" aria-hidden="true">${initial}</span><div><strong>${name}</strong><a href="perfil.html">Ver meu perfil</a></div></div>
                <a href="dashboard.html">Painel</a>
                <p class="nav-menu-label">Produtos</p>
                <a href="meus-pedidos.html">Meus pedidos</a><a href="meus-produtos.html">Meus anúncios</a><a href="minhas-vendas.html">Vendas</a>
                <a href="negociacoes.html">Negociações</a><a href="comparar.html">Comparar produtos</a>
                <p class="nav-menu-label">Serviços e projetos</p>
                <a href="servicos-contratados.html">Solicitações de serviço</a><a href="meus-projetos.html">Meus projetos</a><a href="propostas.html">Minhas propostas</a>
                <span class="nav-menu-separator" aria-hidden="true"></span>
                <button id="nav-logout" type="button">Sair da conta</button>
            </div>
        </details>` : `<a href="login.html">Entrar</a><a href="cadastro.html" class="btn nav-signup">Criar conta</a>`;

    nav.innerHTML = `
        <a href="produtos.html">Produtos</a>
        <a href="servicos.html">Serviços</a>
        <a href="projetos.html">Projetos</a>
        <span class="nav-divider" aria-hidden="true"></span>
        ${personal}
        ${typeof cartControl === 'function' ? cartControl() : ''}
        ${account}
    `;

    syncFooter(authenticated);

    // Páginas internas destacam a seção a que pertencem.
    const currentFile = location.pathname.split('/').pop() || 'index.html';
    const sectionOf = {
        'produto.html': 'produtos.html', 'servico.html': 'servicos.html', 'projeto.html': 'projetos.html',
        'publicar-projeto.html': 'projetos.html', 'publicar-produto.html': 'vender.html',
        'editar-produto.html': 'vender.html', 'oferecer-servico.html': 'vender.html'
    };
    const activePage = sectionOf[currentFile] || currentFile;
    nav.querySelectorAll('a[href]').forEach(link => {
        if (link.getAttribute('href') === activePage) {
            link.classList.add('is-current');
            link.setAttribute('aria-current', 'page');
        }
    });

    document.getElementById('nav-logout')?.addEventListener('click', () => {
        localStorage.removeItem('token');
        sessionStorage.removeItem('token');
        localStorage.removeItem('userName');
        location.href = 'index.html';
    });
    if (authenticated) apiCall('/notifications/unread-count').then(result => {
        const badge = document.getElementById('nav-notification-count');
        if (badge && Number(result.count) > 0) { badge.hidden = false; badge.textContent = Number(result.count) > 99 ? '99+' : String(result.count); }
    }).catch(() => {});
    nav.querySelectorAll('a').forEach(link => link.addEventListener('click', () => {
        header?.classList.remove('nav-open');
        document.getElementById('nav-toggle')?.setAttribute('aria-expanded', 'false');
    }));

    const accountMenu = nav.querySelector('.nav-account');
    document.addEventListener('click', event => {
        if (accountMenu?.open && !accountMenu.contains(event.target)) accountMenu.open = false;
    });
    document.addEventListener('keydown', event => {
        if (event.key === 'Escape' && accountMenu?.open) accountMenu.open = false;
    });
}

// O footer é HTML estático em cada página; aqui ele só ganha o ano atual e os atalhos de quem está logado.
function syncFooter(authenticated) {
    const year = document.getElementById('footer-year');
    if (year) year.textContent = String(new Date().getFullYear());
    const account = document.querySelector('[data-footer-account]');
    if (account && authenticated) {
        account.innerHTML = '<h2>Sua conta</h2><a href="dashboard.html">Painel</a><a href="meus-pedidos.html">Meus pedidos</a><a href="mensagens.html">Mensagens</a>';
    }
}

function cartControl() {
    return `<button class="nav-cart-toggle" id="nav-cart-toggle" type="button" aria-label="Abrir carrinho, 0 itens" aria-controls="cart-drawer" aria-expanded="false">
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M3 4h2l2.1 10.1a2 2 0 0 0 2 1.6h7.8a2 2 0 0 0 1.9-1.4L21 8H6"/><circle cx="10" cy="20" r="1"/><circle cx="18" cy="20" r="1"/></svg>
        <span class="nav-cart-label">Carrinho</span><span class="nav-cart-count" id="nav-cart-count">0</span>
    </button>`;
}

document.addEventListener('DOMContentLoaded', renderNav);
