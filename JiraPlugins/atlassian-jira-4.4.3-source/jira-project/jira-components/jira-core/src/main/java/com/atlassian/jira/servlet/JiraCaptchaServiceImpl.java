package com.atlassian.jira.servlet;

import com.octo.captcha.engine.CaptchaEngine;
import com.octo.captcha.engine.image.gimpy.GmailEngine;
import com.octo.captcha.service.captchastore.MapCaptchaStore;
import com.octo.captcha.service.image.DefaultManageableImageCaptchaService;
import com.octo.captcha.service.image.ImageCaptchaService;

public final class JiraCaptchaServiceImpl implements JiraCaptchaService
{
    private static final DefaultManageableImageCaptchaService instance;

    static
    {
        final MapCaptchaStore store = new MapCaptchaStore();
        final CaptchaEngine engine = new GmailEngine();
        //
        // The numerical parameters are taken from the default constructor as well
        //
        // minGuarantedStorageDelayInSeconds = 180s
        // maxCaptchaStoreSize = 100000
        // captchaStoreLoadBeforeGarbageCollection=75000
        //
        instance = new DefaultManageableImageCaptchaService(store, engine, 180, 100000, 75000);
    }

    public static ImageCaptchaService getInstance()
    {
        return instance;
    }

    public ImageCaptchaService getImageCaptchaService()
    {
        return getInstance();
    }
}
