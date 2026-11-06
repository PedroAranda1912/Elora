document.addEventListener('DOMContentLoaded', async () => {
    if (!requireAuth()) return;
    const id = new URLSearchParams(location.search).get('id');
    const box = document.getElementById('order-detail'); const message = document.getElementById('order-detail-message');
    if (!id || !/^\d+$/.test(id)) { message.textContent = 'Pedido não encontrado.'; return; }
    try {
        const [order, profile] = await Promise.all([apiCall(`/orders/${id}`), apiCall('/users/me')]);
        const buyer = order.buyer?.id === profile.id;
        const ownsListing = order.items?.some(item => item.product?.owner?.id === profile.id);
        const items = order.items || [];
        const reviewStatuses = buyer && order.status === 'CONCLUIDO' ? await apiCall(`/reviews/order/${order.id}`) : [];
        const paymentLabel = order.paymentMethod === 'PIX' ? 'PIX (simulado)' : 'Cart&atilde;o (simulado)';
        const formattedAddress = escapeHtml(order.deliveryAddress || '').replace(/\s*\u00b7\s*/g, '<br>');
        const shippingCard = order.deliveryAddress ? `<section class="order-info-card"><div class="order-info-card-heading"><span class="order-info-icon" aria-hidden="true">&#8982;</span><div><span>ENTREGA</span><h2>Endere&ccedil;o</h2></div></div><p>${formattedAddress}</p></section>` : '';
        const actionMarkup = `${buyer && order.status === 'ENVIADO' ? '<button class="btn" data-next-status="CONCLUIDO">Confirmar recebimento</button>' : ''}${ownsListing && ['PAGAMENTO_CONFIRMADO', 'ENVIADO'].includes(order.status) ? '<button class="btn" data-next-status="ENVIADO">Marcar como enviado</button>' : ''}${buyer && order.status === 'CONCLUIDO' ? reviewStatuses.map(status => status.reviewed ? `<span class="review-complete" aria-label="Avalia&ccedil;&atilde;o enviada">&#10003; Avalia&ccedil;&atilde;o enviada &middot; ${escapeHtml(status.sellerName)}</span>` : `<button class="btn btn-secondary" data-product-review="${status.sellerId}" data-seller-name="${escapeHtml(status.sellerName)}">Avaliar ${escapeHtml(status.sellerName)}</button>`).join('') : ''}`;
        box.innerHTML = `
            <section class="order-overview">
                <div class="order-overview-heading">
                    <div><span class="badge badge-status">${escapeHtml(order.status.replaceAll('_', ' '))}</span><h1>Pedido #${order.id}</h1></div>
                    <div class="order-overview-total"><span>Total do pedido</span><strong>${formatCurrency(order.totalAmount)}</strong></div>
                </div>
                <time class="order-created-at" datetime="${escapeHtml(order.createdAt)}">${new Date(order.createdAt).toLocaleString('pt-BR')}</time>
            </section>
            <section class="order-section">
                <div class="order-section-heading"><h2>Itens do pedido</h2><span>${items.length} ${items.length === 1 ? 'item' : 'itens'}</span></div>
                <div class="order-detail-items">${items.map(item => `<article class="order-detail-item">
                    <a class="order-detail-image" href="produto.html?id=${item.product?.id}" aria-label="Ver ${escapeHtml(item.product?.name)}"><img src="${escapeHtml(item.product?.imageUrl || FALLBACK_IMG)}" alt="${escapeHtml(item.product?.name)}" loading="lazy"></a>
                    <div class="order-item-copy"><h3><a href="produto.html?id=${item.product?.id}">${escapeHtml(item.product?.name)}</a></h3>
                    <p>Vendido por ${escapeHtml(item.product?.owner?.name || 'anunciante')} <span aria-hidden="true">&middot;</span> ${item.quantity} ${item.quantity === 1 ? 'unidade' : 'unidades'}</p>
                    <a class="order-seller-link" href="usuario.html?id=${item.product?.owner?.id}">Ver perfil do vendedor</a></div>
                    <div class="order-item-price"><span>Subtotal</span><strong>${formatCurrency(item.subtotal)}</strong></div>
                </article>`).join('')}</div>
            </section>
            <section class="order-section order-info-section">
                <div class="order-section-heading"><h2>Detalhes da compra</h2></div>
                <div class="order-info-grid">
                    <section class="order-info-card"><div class="order-info-card-heading"><span class="order-info-icon" aria-hidden="true">R$</span><div><span>PAGAMENTO</span><h2>Forma de pagamento</h2></div></div><p>${paymentLabel}</p></section>
                    ${shippingCard}
                    <section class="order-info-card order-total-card"><div><span>VALOR PAGO</span><strong>${formatCurrency(order.totalAmount)}</strong></div><span class="order-total-check" aria-hidden="true">&#10003;</span></section>
                </div>
            </section>
            ${actionMarkup ? `<div class="order-detail-actions">${actionMarkup}</div>` : ''}`;
        message.hidden = true; box.hidden = false;
        box.querySelectorAll('[data-next-status]').forEach(button => button.addEventListener('click', async () => {
            setButtonLoading(button, true, 'Atualizando…');
            try { await apiCall(`/orders/${order.id}/status`, 'PATCH', { status: button.dataset.nextStatus }); showToast('Pedido atualizado.'); location.reload(); }
            catch (error) { showToast(error.message || 'Não foi possível atualizar o pedido.', 'error'); setButtonLoading(button, false); }
        }));
        box.querySelectorAll('[data-product-review]').forEach(button => button.addEventListener('click', () => productReviewDialog(order.id, Number(button.dataset.productReview), button.dataset.sellerName)));
    } catch (error) { message.className = 'alert error'; message.textContent = error.message || 'Não foi possível carregar este pedido.'; }
});

function productReviewDialog(orderId, sellerId, sellerName) {
    const dialog = document.createElement('dialog'); dialog.className = 'elora-dialog';
    dialog.innerHTML = `<form class="review-dialog-form"><button type="button" class="dialog-close" aria-label="Fechar">×</button><p class="catalog-eyebrow">Sua compra · vendedor</p><h2>Como foi sua experiência com ${escapeHtml(sellerName)}?</h2><fieldset class="review-star-picker"><legend>Como você avalia este vendedor?</legend><div role="radiogroup" aria-label="Nota de 1 a 5">${[1,2,3,4,5].map(value => `<button type="button" role="radio" aria-checked="false" aria-label="${value} ${value === 1 ? 'estrela' : 'estrelas'}" data-rating="${value}">★</button>`).join('')}</div><input type="hidden" name="rating" value="0"></fieldset><label>Conte como foi sua experiência com este vendedor<textarea name="comment" rows="4" required maxlength="2000" placeholder="Seu comentário ajuda outras pessoas a comprar com confiança."></textarea></label><div class="alert" role="status"></div><div class="review-dialog-actions"><button class="review-later" type="button">Deixar para depois</button><button class="btn" type="submit">Enviar avaliação</button></div></form>`;
    document.body.append(dialog); dialog.showModal(); dialog.querySelector('.dialog-close').onclick = () => dialog.close(); dialog.addEventListener('close', () => dialog.remove());
    dialog.querySelector('.review-later').onclick = () => dialog.close();
    const picker = dialog.querySelector('.review-star-picker');
    const chooseRating = rating => { picker.querySelector('input').value = String(rating); picker.querySelectorAll('[data-rating]').forEach(star => { const selected = Number(star.dataset.rating) <= rating; star.classList.toggle('is-selected', selected); star.setAttribute('aria-checked', String(Number(star.dataset.rating) === rating)); }); };
    picker.querySelectorAll('[data-rating]').forEach(star => { star.addEventListener('click', () => chooseRating(Number(star.dataset.rating))); star.addEventListener('mouseenter', () => picker.querySelectorAll('[data-rating]').forEach(item => item.classList.toggle('is-preview', Number(item.dataset.rating) <= Number(star.dataset.rating)))); });
    picker.addEventListener('mouseleave', () => picker.querySelectorAll('[data-rating]').forEach(item => item.classList.remove('is-preview')));
    dialog.querySelector('form').onsubmit = async event => {
        event.preventDefault(); const form = event.currentTarget; const button = form.querySelector('[type="submit"]'); setButtonLoading(button, true, 'Publicando…');
        try { await apiCall('/reviews', 'POST', { type: 'PRODUCT', orderId, reviewedUserId: sellerId, rating: Number(form.elements.rating.value), comment: form.elements.comment.value }); dialog.close(); const reviewButton = document.querySelector(`[data-product-review="${sellerId}"]`); if (reviewButton) { reviewButton.className = 'review-complete'; reviewButton.removeAttribute('data-product-review'); reviewButton.textContent = `✓ Avaliação enviada · ${sellerName}`; } showToast('Avaliação publicada.'); }
        catch (error) { const feedback = form.querySelector('.alert'); feedback.className = 'alert error'; feedback.textContent = error.message; }
        finally { setButtonLoading(button, false); }
    };
}
