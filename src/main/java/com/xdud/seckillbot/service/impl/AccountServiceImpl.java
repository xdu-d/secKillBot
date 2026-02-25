package com.xdud.seckillbot.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xdud.seckillbot.api.dto.request.AccountCreateRequest;
import com.xdud.seckillbot.common.exception.BizException;
import com.xdud.seckillbot.common.exception.ErrorCode;
import com.xdud.seckillbot.domain.entity.Account;
import com.xdud.seckillbot.domain.enums.AccountStatus;
import com.xdud.seckillbot.domain.mapper.AccountMapper;
import com.xdud.seckillbot.security.AesGcmCrypto;
import com.xdud.seckillbot.service.AccountService;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountMapper accountMapper;
    private final AesGcmCrypto aesGcmCrypto;

    public AccountServiceImpl(AccountMapper accountMapper, AesGcmCrypto aesGcmCrypto) {
        this.accountMapper = accountMapper;
        this.aesGcmCrypto = aesGcmCrypto;
    }

    @Override
    public IPage<Account> listAccounts(Page<Account> page) {
        IPage<Account> result = accountMapper.selectPage(page, null);
        result.getRecords().forEach(this::maskCredential);
        return result;
    }

    @Override
    public Account getAccountById(Long id) {
        Account account = accountMapper.selectById(id);
        if (account == null) {
            throw new BizException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        maskCredential(account);
        return account;
    }

    @Override
    public Account createAccount(AccountCreateRequest request) {
        Account account = new Account();
        account.setPlatformType(request.getPlatformType());
        account.setName(request.getName());
        account.setPhone(request.getPhone());
        account.setCredentialJson(aesGcmCrypto.encrypt(request.getCredentialJson()));
        account.setStatus(AccountStatus.ACTIVE);
        account.setRemark(request.getRemark());
        accountMapper.insert(account);
        maskCredential(account);
        return account;
    }

    @Override
    public void deleteAccount(Long id) {
        if (accountMapper.selectById(id) == null) {
            throw new BizException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        accountMapper.deleteById(id);
    }

    @Override
    public void refreshAuth(Long id) {
        Account account = accountMapper.selectById(id);
        if (account == null) {
            throw new BizException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        // TODO: Phase 2 接入 PlatformAdapter.AuthProvider 实现主动刷新
        throw new BizException(ErrorCode.INTERNAL_ERROR, "认证刷新功能将在 Phase 2 实现");
    }

    private void maskCredential(Account account) {
        account.setCredentialJson("***");
        account.setAuthContext(null);
    }
}
