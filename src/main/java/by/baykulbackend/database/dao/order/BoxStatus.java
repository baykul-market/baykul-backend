package by.baykulbackend.database.dao.order;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Box status enum")
public enum BoxStatus {
    CREATED,
    TO_ORDER,
    ON_WAY,
    ARRIVED,
    IN_WAREHOUSE,
    SHIPPED,
    DELIVERED,
    RETURNED,
    CANCELLED;

    /**
     * Compares this status with another based on declaration order
     * @param other the status to compare with
     * @return negative if this is before other, 0 if equal, positive if this is after other
     */
    public int compare(BoxStatus other) {
        return Integer.compare(this.ordinal(), other.ordinal());
    }
}
