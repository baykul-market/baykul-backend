package by.baykulbackend.database.repository.bill;

import by.baykulbackend.database.dao.bill.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IBillRepository extends JpaRepository<Bill, UUID> {}
