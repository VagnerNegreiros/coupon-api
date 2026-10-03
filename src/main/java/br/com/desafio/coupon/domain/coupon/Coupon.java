package br.com.desafio.coupon.domain.coupon;

import br.com.desafio.coupon.domain.exception.CouponAlreadyDeletedException;
import br.com.desafio.coupon.domain.exception.InvalidCouponException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root do contexto de cupons.
 *
 * <p>Todas as regras de negócio vivem aqui (ou nos value objects que o compõem). O estado só muda
 * através de comportamentos ({@link #create}, {@link #delete}) — não existem setters públicos, então
 * nenhuma camada externa consegue colocar um cupom em estado inválido.
 */
public final class Coupon {

    private final UUID id;
    private final CouponCode code;
    private final String description;
    private final DiscountValue discountValue;
    private final Instant expirationDate;
    private final boolean published;
    private final boolean redeemed;
    private final Instant createdAt;
    private final Long version;
    private CouponStatus status;
    private Instant deletedAt;

    private Coupon(UUID id, CouponCode code, String description, DiscountValue discountValue,
                   Instant expirationDate, CouponStatus status, boolean published, boolean redeemed,
                   Instant createdAt, Instant deletedAt, Long version) {
        this.id = id;
        this.code = code;
        this.description = description;
        this.discountValue = discountValue;
        this.expirationDate = expirationDate;
        this.status = status;
        this.published = published;
        this.redeemed = redeemed;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
        this.version = version;
    }

    /**
     * Cria um novo cupom aplicando todas as regras de cadastro.
     *
     * @param now instante atual, recebido de fora para manter o domínio determinístico e testável
     */
    public static Coupon create(String code, String description, BigDecimal discountValue,
                                Instant expirationDate, boolean published, Instant now) {
        CouponCode couponCode = CouponCode.of(code);
        requireText(description, "description");
        DiscountValue discount = DiscountValue.of(discountValue);
        requireFutureOrPresent(expirationDate, now);

        return new Coupon(UUID.randomUUID(), couponCode, description, discount, expirationDate,
                CouponStatus.ACTIVE, published, false, now, null, null);
    }

    /**
     * Reconstrói um cupom já existente (ex.: lido do banco). Não reaplica regras de cadastro, pois um
     * cupom válido no passado pode, legitimamente, estar expirado hoje.
     */
    public static Coupon restore(UUID id, String code, String description, BigDecimal discountValue,
                                 Instant expirationDate, CouponStatus status, boolean published,
                                 boolean redeemed, Instant createdAt, Instant deletedAt, Long version) {
        return new Coupon(Objects.requireNonNull(id), new CouponCode(code), description,
                new DiscountValue(discountValue), expirationDate, Objects.requireNonNull(status),
                published, redeemed, createdAt, deletedAt, version);
    }

    /**
     * Soft delete: o cupom é marcado como DELETED e todos os dados do cadastro são preservados.
     *
     * @throws CouponAlreadyDeletedException se o cupom já tiver sido deletado
     */
    public void delete(Instant now) {
        if (isDeleted()) {
            throw new CouponAlreadyDeletedException(id);
        }
        this.status = CouponStatus.DELETED;
        this.deletedAt = now;
    }

    public boolean isDeleted() {
        return status == CouponStatus.DELETED;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidCouponException(field, "O campo '%s' é obrigatório.".formatted(field));
        }
    }

    private static void requireFutureOrPresent(Instant expirationDate, Instant now) {
        if (expirationDate == null) {
            throw new InvalidCouponException("expirationDate", "O campo 'expirationDate' é obrigatório.");
        }
        if (expirationDate.isBefore(now)) {
            throw new InvalidCouponException("expirationDate",
                    "A data de expiração não pode estar no passado.");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code.value();
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getDiscountValue() {
        return discountValue.value();
    }

    public Instant getExpirationDate() {
        return expirationDate;
    }

    public CouponStatus getStatus() {
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

    /** Versão para controle de concorrência otimista; nula enquanto o cupom não foi persistido. */
    public Long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Coupon other && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
