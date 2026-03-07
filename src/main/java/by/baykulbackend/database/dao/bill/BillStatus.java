package by.baykulbackend.database.dao.bill;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Box status enum")
public enum BillStatus {
    DRAFT, APPLIED
}
