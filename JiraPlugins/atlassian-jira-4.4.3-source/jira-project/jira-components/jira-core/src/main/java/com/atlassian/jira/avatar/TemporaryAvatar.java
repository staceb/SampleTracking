package com.atlassian.jira.avatar;

import com.atlassian.jira.util.dbc.Assertions;

import java.io.File;

/**
 * Simple immutable bean for holding uploaded but not yet scaled or cropped image files to be used as Avatars.
 *
 * @since v4.0
 */
public final class TemporaryAvatar
{
    private final String contentType;
    private final String originalFilename;
    private final File file;

    public TemporaryAvatar(final String contentType, final String originalFilename, final File file)
    {
        Assertions.notNull("contentType", contentType);
        Assertions.notNull("originalFilename", originalFilename);
        Assertions.notNull("file", file);
        this.contentType = contentType;
        this.originalFilename = originalFilename;
        this.file = file;
    }

    public String getContentType()
    {
        return contentType;
    }

    public String getOriginalFilename()
    {
        return originalFilename;
    }

    public File getFile()
    {
        return file;
    }
}
