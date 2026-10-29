package de.jcss.hbt.application.onetimeitem;

import de.jcss.hbt.domain.exception.OneTimeItemNotFoundException;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.onetimeitem.OneTimeItem;
import de.jcss.hbt.domain.onetimeitem.valueobject.OneTimeItemId;
import de.jcss.hbt.domain.onetimeitem.OneTimeItemRepository;
import de.jcss.hbt.domain.onetimeitem.command.ChangeOneTimeItemCommand;
import de.jcss.hbt.domain.onetimeitem.command.CreateOneTimeItemCommand;
import de.jcss.hbt.domain.onetimeitem.command.DeleteOneTimeItemCommand;
import de.jcss.hbt.domain.onetimeitem.overview.OneTimeItemOverview;
import de.jcss.hbt.domain.onetimeitem.overview.OneTimeItemOverviewQueryHandler;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.Objects;

/**
 * Entry point for all use cases of one-time items (EP-1 to EP-17).
 * <p>
 * Every use case acts on behalf of the owner given in the command. An item of someone else is
 * treated exactly like one that does not exist (EP-4); whether it belongs to a user is decided by
 * the item itself ({@link OneTimeItem#isOwnedBy}). One-time items do not depend on household plans:
 * none of these use cases looks at a plan (EP-5).
 * <p>
 * Write side: each command runs in its own transaction: load the {@link OneTimeItem}, let it apply
 * the change, save it and publish the domain events it recorded. Read side: the
 * {@link OneTimeItemOverview} of a year is loaded through its {@link OneTimeItemOverviewQueryHandler}.
 */
@Service
@Transactional
public class OneTimeItemApplicationService {

    private final OneTimeItemRepository repository;
    private final OneTimeItemOverviewQueryHandler overviewQueryHandler;
    private final ApplicationEventPublisher eventPublisher;

    public OneTimeItemApplicationService(OneTimeItemRepository repository,
                                         OneTimeItemOverviewQueryHandler overviewQueryHandler,
                                         ApplicationEventPublisher eventPublisher) {
        this.repository = Objects.requireNonNull(repository);
        this.overviewQueryHandler = Objects.requireNonNull(overviewQueryHandler);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    /** Enters a one-time item, whether or not the user has a plan for its year (EP-5, EP-7). */
    public OneTimeItemId createItem(CreateOneTimeItemCommand command) {
        OneTimeItem item = OneTimeItem.create(OneTimeItemId.generate(), command.owner(), command.type(),
                command.description(), command.amount(), command.date(), command.note());
        repository.add(item);
        publishEvents(item);
        return item.id();
    }

    /**
     * Changes a one-time item of the user (EP-7).
     *
     * @throws OneTimeItemNotFoundException if it is not an item of the user
     */
    public void changeItem(ChangeOneTimeItemCommand command) {
        OneTimeItem changed = load(command.itemId(), command.owner())
                .change(command.type(), command.description(), command.amount(), command.date(), command.note());
        repository.save(changed);
        publishEvents(changed);
    }

    /**
     * Deletes a one-time item of the user for good (EP-7, EP-17).
     *
     * @throws OneTimeItemNotFoundException if it is not an item of the user
     */
    public void deleteItem(DeleteOneTimeItemCommand command) {
        OneTimeItem item = load(command.itemId(), command.owner());
        item.delete();
        repository.remove(item);
        publishEvents(item);
    }

    /** The one-time items of the user in a year with their totals (EP-8, EP-9); without items if there are none. */
    @Transactional(readOnly = true)
    public OneTimeItemOverview getOverview(OwnerId owner, Year year) {
        return overviewQueryHandler.findByOwnerAndYear(owner, year);
    }

    private OneTimeItem load(OneTimeItemId id, OwnerId owner) {
        return repository.findById(id)
                .filter(item -> item.isOwnedBy(owner))
                .orElseThrow(() -> new OneTimeItemNotFoundException(id));
    }

    private void publishEvents(OneTimeItem item) {
        item.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }
}
