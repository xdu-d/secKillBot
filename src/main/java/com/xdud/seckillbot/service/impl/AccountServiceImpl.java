package com.xdud.seckillbot.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.xdud.seckillbot.api.dto.request.AccountCreateRequest;
import com.xdud.seckillbot.common.exception.BizException;
import com.xdud.seckillbot.common.exception.ErrorCode;
import com.xdud.seckillbot.domain.entity.Account;
import com.xdud.seckillbot.domain.enums.AccountStatus;
import com.xdud.seckillbot.domain.enums.PlatformType;
import com.xdud.seckillbot.domain.mapper.AccountMapper;
import com.xdud.seckillbot.platform.impl.damai.DamaiAdapter;
import com.xdud.seckillbot.platform.impl.damai.DamaiCredential;
import com.xdud.seckillbot.platform.impl.imoutai.ImoutaiAdapter;
import com.xdud.seckillbot.platform.impl.imoutai.ImoutaiCredential;
import com.xdud.seckillbot.platform.impl.maoyan.MaoyanAdapter;
import com.xdud.seckillbot.platform.impl.maoyan.MaoyanCredential;
import com.xdud.seckillbot.platform.model.AuthContext;
import com.xdud.seckillbot.platform.registry.PlatformRegistry;
import com.xdud.seckillbot.platform.spi.PlatformAdapter;
import com.xdud.seckillbot.security.AesGcmCrypto;
import com.xdud.seckillbot.service.AccountService;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountMapper accountMapper;
    private final AesGcmCrypto aesGcmCrypto;
    private final PlatformRegistry platformRegistry;
    private final Gson gson = new Gson();

    public AccountServiceImpl(AccountMapper accountMapper,
                              AesGcmCrypto aesGcmCrypto,
                              PlatformRegistry platformRegistry) {
        this.accountMapper = accountMapper;
        this.aesGcmCrypto = aesGcmCrypto;
        this.platformRegistry = platformRegistry;
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
        Account account = requireAccount(id);
        String ctxJson = account.getAuthContext();
        if (ctxJson == null || ctxJson.isEmpty()) {
            throw new BizException(ErrorCode.ACCOUNT_AUTH_EXPIRED, "认证上下文为空，请先完成登录");
        }
        AuthContext current = gson.fromJson(aesGcmCrypto.decrypt(ctxJson), AuthContext.class);
        PlatformAdapter adapter = platformRegistry.getAdapter(account.getPlatformType());
        AuthContext refreshed = adapter.refresh(current);
        account.setAuthContext(aesGcmCrypto.encrypt(gson.toJson(refreshed)));
        account.setAuthExpiresAt(refreshed.getExpiresAt());
        accountMapper.updateById(account);
    }

    @Override
    public void sendSmsCode(Long accountId) {
        Account account = requireAccount(accountId);
        String credJson = aesGcmCrypto.decrypt(account.getCredentialJson());

        switch (account.getPlatformType()) {
            case IMOUTAI: {
                ImoutaiCredential cred = gson.fromJson(credJson, ImoutaiCredential.class);
                ImoutaiAdapter adapter = (ImoutaiAdapter) platformRegistry.getAdapter(PlatformType.IMOUTAI);
                adapter.sendSmsCode(cred.getPhone(), cred.getDeviceId());
                break;
            }
            case DAMAI: {
                DamaiCredential cred = gson.fromJson(credJson, DamaiCredential.class);
                DamaiAdapter adapter = (DamaiAdapter) platformRegistry.getAdapter(PlatformType.DAMAI);
                adapter.sendSmsCode(cred.getPhone(), cred);
                break;
            }
            case MAOYAN: {
                MaoyanCredential cred = gson.fromJson(credJson, MaoyanCredential.class);
                MaoyanAdapter adapter = (MaoyanAdapter) platformRegistry.getAdapter(PlatformType.MAOYAN);
                adapter.sendSmsCode(cred.getPhone(), cred);
                break;
            }
            default:
                throw new BizException(ErrorCode.PLATFORM_NOT_SUPPORTED, "该平台暂不支持短信登录");
        }
    }

    @Override
    public Account loginWithSmsCode(Long accountId, String smsCode) {
        Account account = requireAccount(accountId);
        String credJson = aesGcmCrypto.decrypt(account.getCredentialJson());
        AuthContext ctx;

        switch (account.getPlatformType()) {
            case IMOUTAI: {
                ImoutaiCredential cred = gson.fromJson(credJson, ImoutaiCredential.class);
                ImoutaiAdapter adapter = (ImoutaiAdapter) platformRegistry.getAdapter(PlatformType.IMOUTAI);
                ctx = adapter.loginWithSms(cred.getPhone(), smsCode, cred.getDeviceId());
                // 回写 token / userId
                cred.setToken(ctx.getAccessToken());
                cred.setUserId(ctx.getPlatformUserId());
                account.setCredentialJson(aesGcmCrypto.encrypt(gson.toJson(cred)));
                break;
            }
            case DAMAI: {
                DamaiCredential cred = gson.fromJson(credJson, DamaiCredential.class);
                DamaiAdapter adapter = (DamaiAdapter) platformRegistry.getAdapter(PlatformType.DAMAI);
                ctx = adapter.loginWithSms(cred.getPhone(), smsCode, cred);
                // 回写 h5Tk / h5TkEnc
                cred.setH5Tk(ctx.getAccessToken());
                if (ctx.getExtras() != null) {
                    cred.setH5TkEnc(ctx.getExtras().get("h5TkEnc"));
                }
                account.setCredentialJson(aesGcmCrypto.encrypt(gson.toJson(cred)));
                break;
            }
            case MAOYAN: {
                MaoyanCredential cred = gson.fromJson(credJson, MaoyanCredential.class);
                MaoyanAdapter adapter = (MaoyanAdapter) platformRegistry.getAdapter(PlatformType.MAOYAN);
                ctx = adapter.loginWithSms(cred.getPhone(), smsCode, cred);
                // 回写 token / userId
                cred.setToken(ctx.getAccessToken());
                cred.setUserId(ctx.getPlatformUserId());
                account.setCredentialJson(aesGcmCrypto.encrypt(gson.toJson(cred)));
                break;
            }
            default:
                throw new BizException(ErrorCode.PLATFORM_NOT_SUPPORTED, "该平台暂不支持短信登录");
        }

        account.setAuthContext(aesGcmCrypto.encrypt(gson.toJson(ctx)));
        account.setAuthExpiresAt(ctx.getExpiresAt());
        accountMapper.updateById(account);

        maskCredential(account);
        return account;
    }

    private Account requireAccount(Long id) {
        Account account = accountMapper.selectById(id);
        if (account == null) {
            throw new BizException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        return account;
    }

    private void maskCredential(Account account) {
        account.setCredentialJson("***");
        account.setAuthContext(null);
    }
}
