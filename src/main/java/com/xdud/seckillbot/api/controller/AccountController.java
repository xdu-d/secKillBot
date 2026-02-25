package com.xdud.seckillbot.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xdud.seckillbot.api.dto.request.AccountCreateRequest;
import com.xdud.seckillbot.api.dto.response.ApiResponse;
import com.xdud.seckillbot.domain.entity.Account;
import com.xdud.seckillbot.service.AccountService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ApiResponse<IPage<Account>> listAccounts(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(accountService.listAccounts(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<Account> getAccount(@PathVariable Long id) {
        return ApiResponse.ok(accountService.getAccountById(id));
    }

    @PostMapping
    public ApiResponse<Account> createAccount(@RequestBody AccountCreateRequest request) {
        return ApiResponse.ok(accountService.createAccount(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/refresh-auth")
    public ApiResponse<Void> refreshAuth(@PathVariable Long id) {
        accountService.refreshAuth(id);
        return ApiResponse.ok();
    }
}
