package com.familygrowth.web;

import com.familygrowth.application.Stage6Service;
import com.familygrowth.domain.Stage3Models;
import com.familygrowth.domain.Stage3Models.Actor;
import com.familygrowth.domain.Stage6Models.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/families/{familyId}")
class Stage6Controller {
    private final Stage6Service service; Stage6Controller(Stage6Service service){this.service=service;}
    @PostMapping("/reward-products") @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<RewardProduct> product(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId,@Valid @RequestBody ProductRequest request){return ApiResponse.ok(service.createProduct(actor,familyId,request.title(),request.coinCost(),request.stockCount(),request.active()));}
    @GetMapping("/reward-products")
    ApiResponse<List<RewardProduct>> products(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId,@RequestParam(defaultValue="true")boolean activeOnly){return ApiResponse.ok(service.products(actor,familyId,activeOnly));}
    @PostMapping("/children/{childId}/reward-orders") @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<RewardOrder> order(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId,@PathVariable UUID childId,@RequestHeader("Idempotency-Key")String key,@Valid @RequestBody OrderRequest request){return ApiResponse.ok(service.createOrder(actor,familyId,childId,request.productId(),key));}
    @PostMapping("/reward-orders/{orderId}/review")
    ApiResponse<RewardOrder> review(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId,@PathVariable UUID orderId,@RequestHeader("Idempotency-Key")String key,@Valid @RequestBody ReviewRequest request){return ApiResponse.ok(service.reviewOrder(actor,familyId,orderId,request.approved(),key));}
    @PostMapping("/reward-orders/{orderId}/cancel")
    ApiResponse<RewardOrder> cancel(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId,@PathVariable UUID orderId,@RequestHeader("Idempotency-Key")String key){return ApiResponse.ok(service.cancelOrder(actor,familyId,orderId,key));}
    @PostMapping("/children/{childId}/saving/transfers") @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<SavingTransaction> transfer(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId,@PathVariable UUID childId,@RequestHeader("Idempotency-Key")String key,@Valid @RequestBody TransferRequest request){return ApiResponse.ok(service.transferSaving(actor,familyId,childId,request.direction(),request.amount(),key));}
    @GetMapping("/children/{childId}/saving")
    ApiResponse<SavingAccount> saving(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId,@PathVariable UUID childId){return ApiResponse.ok(service.saving(actor,familyId,childId));}
    @PostMapping("/children/{childId}/wishes") @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<Wish> wish(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId,@PathVariable UUID childId,@Valid @RequestBody WishRequest request){return ApiResponse.ok(service.createWish(actor,familyId,childId,request.title(),request.targetAmount()));}
    @PostMapping("/wishes/{wishId}/allocation")
    ApiResponse<Wish> allocate(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId,@PathVariable UUID wishId,@RequestHeader("Idempotency-Key")String key,@Valid @RequestBody AllocationRequest request){return ApiResponse.ok(service.allocateWish(actor,familyId,wishId,request.amount(),key));}
    @GetMapping("/children/{childId}/wishes")
    ApiResponse<List<Wish>> wishes(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId,@PathVariable UUID childId){return ApiResponse.ok(service.wishes(actor,familyId,childId));}
    record ProductRequest(@NotBlank @Size(max=120)String title,@Min(1)long coinCost,@Min(0) @Max(1000000)int stockCount,boolean active){}
    record OrderRequest(@NotNull UUID productId){} record ReviewRequest(boolean approved){}
    record TransferRequest(@NotNull SavingDirection direction,@NotNull @DecimalMin("0.01")BigDecimal amount){}
    record WishRequest(@NotBlank @Size(max=120)String title,@NotNull @DecimalMin("0.01")BigDecimal targetAmount){}
    record AllocationRequest(@NotNull @DecimalMin("0.00")BigDecimal amount){}
}
