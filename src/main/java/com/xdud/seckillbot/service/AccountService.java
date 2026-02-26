package com.xdud.seckillbot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xdud.seckillbot.api.dto.request.AccountCreateRequest;
import com.xdud.seckillbot.domain.entity.Account;

public interface AccountService {

    IPage<Account> listAccounts(Page<Account> page);

    Account getAccountById(Long id);

    Account createAccount(AccountCreateRequest request);

    void deleteAccount(Long id);

    void refreshAuth(Long id);

    void sendSmsCode(Long accountId);

    Account loginWithSmsCode(Long accountId, String smsCode);
}
