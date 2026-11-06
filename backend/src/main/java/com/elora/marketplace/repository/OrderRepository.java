package com.elora.marketplace.repository;
import com.elora.marketplace.model.AppOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<AppOrder, Long> {
    List<AppOrder> findByBuyerId(Long buyerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from AppOrder o where o.id = :id")
    java.util.Optional<AppOrder> findByIdForUpdate(@Param("id") Long id);
    @Query("select distinct o from AppOrder o join o.items i where i.product.owner.id = :sellerId order by o.createdAt desc")
    List<AppOrder> findSalesBySeller(@Param("sellerId") Long sellerId);
}
