package de.jcss.hbt.application.networth;

import de.jcss.hbt.domain.networth.overview.NetWorthOverview;

import java.util.Objects;

/**
 * The result of taking over the assets and liabilities of a year into another one (VS-15).
 *
 * @param overwritten whether the target year had a statement before, which was overwritten (VS-16)
 * @param overview    the target year after the copy
 */
public record CopiedNetWorthStatement(boolean overwritten, NetWorthOverview overview) {

    public CopiedNetWorthStatement {
        Objects.requireNonNull(overview, "overview must not be null");
    }
}
