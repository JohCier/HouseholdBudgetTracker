package de.jcss.hbt.infrastructure.web;

import de.jcss.hbt.application.networth.CopiedNetWorthStatement;
import de.jcss.hbt.application.networth.NetWorthApplicationService;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.networth.NetWorthEntryId;
import de.jcss.hbt.domain.networth.NetWorthEntryName;
import de.jcss.hbt.domain.networth.NetWorthStatement;
import de.jcss.hbt.domain.networth.command.AddNetWorthEntryCommand;
import de.jcss.hbt.domain.networth.command.ChangeNetWorthEntryCommand;
import de.jcss.hbt.domain.networth.command.CopyNetWorthStatementCommand;
import de.jcss.hbt.domain.networth.command.RemoveNetWorthEntryCommand;
import de.jcss.hbt.infrastructure.web.dto.CopyNetWorthRequest;
import de.jcss.hbt.infrastructure.web.dto.NetWorthEntryRequest;
import de.jcss.hbt.infrastructure.web.dto.NetWorthHistoryResponse;
import de.jcss.hbt.infrastructure.web.dto.NetWorthOverviewResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.util.UUID;

/**
 * Exposes the assets and liabilities of the signed-in user (Vermögen und Schulden) as a REST API,
 * addressed by their year. Contains no business logic: changes become commands for the
 * {@link NetWorthApplicationService}, and every change answers with the overview of the year after it.
 */
@RestController
@RequestMapping(path = "/api/net-worth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "net-worth")
@SecurityRequirement(name = OpenApiConfig.SESSION_COOKIE)
public class NetWorthController {

    private final NetWorthApplicationService netWorth;

    public NetWorthController(NetWorthApplicationService netWorth) {
        this.netWorth = netWorth;
    }

    /** All years the user recorded assets or liabilities for, the newest first, with their change from the year before (VS-12, VS-18). */
    @GetMapping("/years")
    public NetWorthHistoryResponse history(@AuthenticationPrincipal Jwt user) {
        return NetWorthHistoryResponse.from(netWorth.getHistory(owner(user)));
    }

    /** The assets and liabilities of a year (VS-8); without entries if the user recorded none for it (VS-10). */
    @GetMapping("/{year}")
    public NetWorthOverviewResponse overview(@AuthenticationPrincipal Jwt user, @PathVariable
                                             @Min(NetWorthStatement.FIRST_YEAR) @Max(NetWorthStatement.LAST_YEAR) int year) {
        return NetWorthOverviewResponse.from(netWorth.getOverview(owner(user), Year.of(year)));
    }

    /** Records an asset or a liability for a year (VS-1, VS-5). */
    @PostMapping("/{year}/entries")
    @ResponseStatus(HttpStatus.CREATED)
    public NetWorthOverviewResponse addEntry(@AuthenticationPrincipal Jwt user, @PathVariable
                                             @Min(NetWorthStatement.FIRST_YEAR) @Max(NetWorthStatement.LAST_YEAR) int year,
                                             @Valid @RequestBody NetWorthEntryRequest request) {
        return NetWorthOverviewResponse.from(netWorth.addEntry(new AddNetWorthEntryCommand(owner(user),
                Year.of(year), request.type().toDomain(), NetWorthEntryName.of(request.name()),
                Money.of(request.amount()))));
    }

    /** Gives an asset or a liability new values (VS-5); 404 if it is not one of the user for the year. */
    @PutMapping("/{year}/entries/{entryId}")
    public NetWorthOverviewResponse changeEntry(@AuthenticationPrincipal Jwt user, @PathVariable
                                                @Min(NetWorthStatement.FIRST_YEAR) @Max(NetWorthStatement.LAST_YEAR) int year,
                                                @PathVariable UUID entryId,
                                                @Valid @RequestBody NetWorthEntryRequest request) {
        return NetWorthOverviewResponse.from(netWorth.changeEntry(new ChangeNetWorthEntryCommand(owner(user),
                Year.of(year), NetWorthEntryId.of(entryId), request.type().toDomain(),
                NetWorthEntryName.of(request.name()), Money.of(request.amount()))));
    }

    /** Removes an asset or a liability (VS-5); 404 if it is not one of the user for the year. */
    @DeleteMapping("/{year}/entries/{entryId}")
    public NetWorthOverviewResponse removeEntry(@AuthenticationPrincipal Jwt user, @PathVariable
                                                @Min(NetWorthStatement.FIRST_YEAR) @Max(NetWorthStatement.LAST_YEAR) int year,
                                                @PathVariable UUID entryId) {
        return NetWorthOverviewResponse.from(netWorth.removeEntry(
                new RemoveNetWorthEntryCommand(owner(user), Year.of(year), NetWorthEntryId.of(entryId))));
    }

    /**
     * Takes over the assets and liabilities of a year into another one (VS-15, VS-16): 201 if the target
     * year had none, 200 if they were overwritten, 409 if it has some and they may not be overwritten.
     */
    @PostMapping("/{year}/copies")
    public ResponseEntity<NetWorthOverviewResponse> copy(@AuthenticationPrincipal Jwt user, @PathVariable
                                                         @Min(NetWorthStatement.FIRST_YEAR) @Max(NetWorthStatement.LAST_YEAR) int year,
                                                         @Valid @RequestBody CopyNetWorthRequest request) {
        CopiedNetWorthStatement copied = netWorth.copyStatement(new CopyNetWorthStatementCommand(owner(user),
                Year.of(year), Year.of(request.targetYear()), Boolean.TRUE.equals(request.overwrite())));
        return ResponseEntity.status(copied.overwritten() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(NetWorthOverviewResponse.from(copied.overview()));
    }

    /** The signed-in user; the subject of their session token is their user id. */
    private static OwnerId owner(Jwt user) {
        return OwnerId.of(UUID.fromString(user.getSubject()));
    }
}
