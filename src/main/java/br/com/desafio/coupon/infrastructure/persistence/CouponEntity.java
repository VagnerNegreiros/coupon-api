package br.com.desafio.coupon.infrastructure.persistence;

import br.com.desafio.coupon.domain.coupon.Coupon;
import br.com.desafio.coupon.domain.coupon.CouponStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de persistência (JPA). Separado da entidade de domínio para que anotações e restrições do
 * ORM (construtor vazio, campos mutáveis) não vazem para as regras de negócio.
 */
@Entity
@Table(name = "coupons")
public class CouponEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false)
    private String description;

    @Column(name = "discount_value", nullable = false)
    private BigDecimal discountValue;

    @Column(name = "expiration_date", nullable = false)
    private Instant expirationDate;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private boolean published;

    @Column(nullable = false)
    private boolean redeemed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Lock otimista: impede que duas operações concorrentes sobrescrevam uma à outra. */
    @Version
    private Long version;

    protected CouponEntity() {
    }

    static CouponEntity fromDomain(Coupon coupon) {
        CouponEntity entity = new CouponEntity();
        entity.id = coupon.getId();
        entity.code = coupon.getCode();
        entity.description = coupon.getDescription();
        entity.discountValue = coupon.getDiscountValue();
        entity.expirationDate = coupon.getExpirationDate();
        entity.status = coupon.getStatus().name();
        entity.published = coupon.isPublished();
        entity.redeemed = coupon.isRedeemed();
        entity.createdAt = coupon.getCreatedAt();
        entity.deletedAt = coupon.getDeletedAt();
        entity.version = coupon.getVersion();
        return entity;
    }

    Coupon toDomain() {
        return Coupon.restore(id, code, description, discountValue, expirationDate,
                CouponStatus.valueOf(status), published, redeemed, createdAt, deletedAt, version);
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public Instant getExpirationDate() {
        return expirationDate;
    }

    public String getStatus() {
        return status;
    }

    public boolean isPublished() {
        return published;
    }

    public boolean isRedeemed() {
        return redeemed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Long getVersion() {
        return version;
    }
}
