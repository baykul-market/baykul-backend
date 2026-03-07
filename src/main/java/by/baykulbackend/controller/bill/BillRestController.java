package by.baykulbackend.controller.bill;

import by.baykulbackend.database.dao.bill.Bill;
import by.baykulbackend.database.dto.security.Views;
import by.baykulbackend.database.repository.bill.IBillRepository;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.bill.BillService;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bill")
@RequiredArgsConstructor
@Tag(name = "Bills", description = "Bills management")
public class BillRestController {
    private final BillService billService;
    private final IBillRepository iBillRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('bills:read')")
    @JsonView(Views.BillView.Get.class)
    public List<Bill> getAll(
            @PageableDefault(size = 50, sort = "createdTs", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return iBillRepository.findAll(pageable).stream().toList();
    }

    @GetMapping("/id")
    @PreAuthorize("hasAnyAuthority('bills:read')")
    @JsonView(Views.BillFullView.class)
    public Bill getOne(
            @Parameter(
                    description = "UUID of the bill to retrieve",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestParam UUID id
    ) {
        return iBillRepository.findById(id).orElseThrow(() -> new NotFoundException("Order not found"));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('bills:write')")
    public ResponseEntity<?> create(
            @JsonView(Views.BillCreateFullView.class)
            @RequestBody
            Bill bill
    ) {
        return billService.createBill(bill);
    }

    @PostMapping("/apply")
    @PreAuthorize("hasAnyAuthority('bills:write')")
    public ResponseEntity<?> apply(
            @Parameter(
                    description = "UUID of the bill to apply",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestParam UUID id
    ) {
        return billService.applyBill(id);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('bills:write')")
    public ResponseEntity<?> add(
            @Parameter(
                    description = "UUID of the order product to add to bill",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestParam UUID orderProductId,
            @Parameter(
                    description = "UUID of the bill",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestParam UUID billId
    ) {
        return billService.addBoxToBill(billId, orderProductId);
    }

    @PostMapping("/remove")
    @PreAuthorize("hasAnyAuthority('bills:write')")
    public ResponseEntity<?> remove(
            @Parameter(
                    description = "UUID of the order product to remove from bill",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestParam UUID orderProductId,
            @Parameter(
                    description = "UUID of the bill",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestParam UUID billId
    ) {
        return billService.removeBoxFromBill(billId, orderProductId);
    }

    @DeleteMapping
    @PreAuthorize("hasAnyAuthority('bills:write')")
    public ResponseEntity<?> delete(
            @Parameter(
                    description = "UUID of the bill to delete",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestParam UUID id
    ) {
        return billService.deleteBillById(id);
    }
}
