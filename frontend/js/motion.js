(() => {
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    const revealSelector = [
        'main > .page-intro',
        'main > section',
        'main > .section-heading',
        'main .card',
        'main .project-opportunity-card',
        'main .service-market-card',
        'main .notification-item',
        'main .negotiation-item'
    ].join(',');

    function initScrollReveals() {
        const main = document.querySelector('main');
        if (!main || reducedMotion.matches || !('IntersectionObserver' in window)) return;

        const revealObserver = new IntersectionObserver(entries => {
            entries.forEach(entry => {
                if (!entry.isIntersecting) return;
                entry.target.classList.remove('reveal-pending');
                entry.target.classList.add('is-revealed');
                revealObserver.unobserve(entry.target);
            });
        }, { threshold: 0.06, rootMargin: '0px 0px -36px 0px' });

        const observeTarget = target => {
            if (reducedMotion.matches || !(target instanceof HTMLElement) || !target.matches(revealSelector)
                || target.closest('[hidden]') || target.classList.contains('reveal-pending')
                || target.classList.contains('is-revealed')) return;

            const bounds = target.getBoundingClientRect();
            if (bounds.top < window.innerHeight && bounds.bottom > 0) return;

            const siblings = Array.from(target.parentElement?.children || [])
                .filter(sibling => sibling.matches(revealSelector));
            const delay = Math.min(Math.max(siblings.indexOf(target), 0) * 55, 220);
            target.style.setProperty('--reveal-delay', `${delay}ms`);
            target.classList.add('reveal-pending');
            revealObserver.observe(target);
        };

        const scan = node => {
            if (!(node instanceof HTMLElement)) return;
            observeTarget(node);
            node.querySelectorAll(revealSelector).forEach(observeTarget);
        };

        scan(main);
        const contentObserver = new MutationObserver(records => {
            records.forEach(record => {
                if (record.type === 'attributes') scan(record.target);
                record.addedNodes.forEach(scan);
            });
        });
        contentObserver.observe(main, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ['hidden']
        });

        reducedMotion.addEventListener('change', event => {
            if (!event.matches) return;
            main.querySelectorAll('.reveal-pending').forEach(target => {
                target.classList.remove('reveal-pending');
                target.classList.add('is-revealed');
                revealObserver.unobserve(target);
            });
        });
    }

    document.addEventListener('DOMContentLoaded', () => {
        initScrollReveals();
    }, { once: true });
})();
