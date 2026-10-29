package de.jcss.hbt.application.networth;

import de.jcss.hbt.domain.exception.NetWorthStatementAlreadyExistsException;
import de.jcss.hbt.domain.exception.NetWorthStatementNotFoundException;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.networth.NetWorthStatement;
import de.jcss.hbt.domain.networth.NetWorthStatementRepository;
import de.jcss.hbt.domain.networth.command.AddNetWorthEntryCommand;
import de.jcss.hbt.domain.networth.command.ChangeNetWorthEntryCommand;
import de.jcss.hbt.domain.networth.command.CopyNetWorthStatementCommand;
import de.jcss.hbt.domain.networth.command.RemoveNetWorthEntryCommand;
import de.jcss.hbt.domain.networth.overview.NetWorthHistory;
import de.jcss.hbt.domain.networth.overview.NetWorthOverview;
import de.jcss.hbt.domain.networth.overview.NetWorthQueryHandler;
import de.jcss.hbt.domain.networth.valueobject.NetWorthStatementId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.Objects;
import java.util.Optional;

/**
 * Entry point for all use cases of assets and liabilities (Vermögen und Schulden).
 * <p>
 * Every use case acts on behalf of a user, the owner given in the command, and addresses a statement
 * by its year. Users only ever get to see their own statements: the repository is asked for the
 * statement of the owner, so a statement of someone else is never found.
 * <p>
 * Write side: each command runs in its own transaction: load the {@link NetWorthStatement} of the
 * year, let it apply the change, save it, and publish the domain events it recorded. Every command
 * answers with the overview of the year after the change, read within the same transaction (like
 * AU-1). A year gets its statement with its first entry and loses it with its last one (VS-7).
 * <p>
 * Contains no business rules of its own. Whether the target year of a copy already has a statement is a
 * process step deciding whether to create or to overwrite one; the unique constraint behind
 * {@link NetWorthStatementRepository#add} remains the safeguard against races.
 */
@Service
@Transactional
public class NetWorthApplicationService {

    private final NetWorthStatementRepository repository;
    private final NetWorthQueryHandler queryHandler;
    private final ApplicationEventPublisher eventPublisher;

    public NetWorthApplicationService(NetWorthStatementRepository repository, NetWorthQueryHandler queryHandler,
                                      ApplicationEventPublisher eventPublisher) {
        this.repository = Objects.requireNonNull(repository);
        this.queryHandler = Objects.requireNonNull(queryHandler);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    /** Records an asset or a liability; the first one of a year creates its statement (VS-5, VS-7). */
    public NetWorthOverview addEntry(AddNetWorthEntryCommand command) {
        Optional<NetWorthStatement> existing = repository.findFor(command.owner(), command.year());
        NetWorthStatement statement = existing.orElseGet(() ->
                NetWorthStatement.create(NetWorthStatementId.generate(), command.owner(), command.year()));
        statement.addEntry(command.type(), command.name(), command.amount());
        if (existing.isPresent()) {
            repository.save(statement);
        } else {
            repository.add(statement);
        }
        publishEvents(statement);
        return overviewOf(command.owner(), command.year());
    }

    /** @throws NetWorthStatementNotFoundException if the user recorded nothing for the year */
    public NetWorthOverview changeEntry(ChangeNetWorthEntryCommand command) {
        NetWorthStatement statement = load(command.owner(), command.year());
        statement.changeEntry(command.entryId(), command.type(), command.name(), command.amount());
        repository.save(statement);
        publishEvents(statement);
        return overviewOf(command.owner(), command.year());
    }

    /**
     * Removes an entry; with the last one the year loses its statement (VS-7).
     *
     * @throws NetWorthStatementNotFoundException if the user recorded nothing for the year
     */
    public NetWorthOverview removeEntry(RemoveNetWorthEntryCommand command) {
        NetWorthStatement statement = load(command.owner(), command.year());
        statement.removeEntry(command.entryId());
        if (statement.isEmpty()) {
            repository.remove(statement);
        } else {
            repository.save(statement);
        }
        publishEvents(statement);
        return overviewOf(command.owner(), command.year());
    }

    /**
     * Copies the statement of a year of the user to the target year: into a new statement if the user
     * has none for it yet, otherwise over the one they have, but only if the command allows that (VS-15,
     * VS-16). Only the statement of the target year is changed; the source is just read.
     *
     * @throws NetWorthStatementNotFoundException      if the user recorded nothing for the source year
     * @throws NetWorthStatementAlreadyExistsException if the target year has a statement that may not be overwritten
     */
    public CopiedNetWorthStatement copyStatement(CopyNetWorthStatementCommand command) {
        NetWorthStatement source = load(command.owner(), command.sourceYear());
        source.requireCopyableTo(command.targetYear());
        Optional<NetWorthStatement> existing = repository.findFor(command.owner(), command.targetYear());
        NetWorthStatement target;
        if (existing.isEmpty()) {
            target = NetWorthStatement.createFrom(source, NetWorthStatementId.generate(), command.targetYear());
            repository.add(target);
        } else if (command.overwrite()) {
            target = existing.get();
            target.overwriteWith(source);
            repository.save(target);
        } else {
            throw new NetWorthStatementAlreadyExistsException(command.targetYear());
        }
        publishEvents(target);
        return new CopiedNetWorthStatement(existing.isPresent(), overviewOf(command.owner(), command.targetYear()));
    }

    /** The assets and liabilities of the user for the year; without entries if they recorded none (VS-10). */
    @Transactional(readOnly = true)
    public NetWorthOverview getOverview(OwnerId owner, Year year) {
        NetWorthStatement.requireYearInRange(year);
        return overviewOf(owner, year);
    }

    /** All years the user recorded assets or liabilities for, the newest first; only reachable years (VS-2, VS-18). */
    @Transactional(readOnly = true)
    public NetWorthHistory getHistory(OwnerId owner) {
        return queryHandler.findHistoryByOwner(owner,
                Year.of(NetWorthStatement.FIRST_YEAR), Year.of(NetWorthStatement.LAST_YEAR));
    }

    private NetWorthStatement load(OwnerId owner, Year year) {
        return repository.findFor(owner, year)
                .filter(statement -> statement.isOwnedBy(owner))
                .orElseThrow(() -> new NetWorthStatementNotFoundException(year));
    }

    private NetWorthOverview overviewOf(OwnerId owner, Year year) {
        return queryHandler.findByOwnerAndYear(owner, year);
    }

    private void publishEvents(NetWorthStatement statement) {
        statement.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }
}
