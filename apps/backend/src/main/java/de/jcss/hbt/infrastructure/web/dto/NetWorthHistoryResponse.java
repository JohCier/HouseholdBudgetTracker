package de.jcss.hbt.infrastructure.web.dto;

import de.jcss.hbt.domain.networth.overview.NetWorthHistory;

import java.util.List;

/** All years the user recorded assets or liabilities for, the newest first (VS-18). */
public record NetWorthHistoryResponse(List<NetWorthHistoryEntryResponse> years) {

    public static NetWorthHistoryResponse from(NetWorthHistory history) {
        return new NetWorthHistoryResponse(history.years().stream().map(NetWorthHistoryEntryResponse::from).toList());
    }
}
