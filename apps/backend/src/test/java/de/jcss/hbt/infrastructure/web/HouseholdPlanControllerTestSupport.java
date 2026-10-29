package de.jcss.hbt.infrastructure.web;

import de.jcss.hbt.application.householdplan.AddedBudgetItem;
import de.jcss.hbt.application.householdplan.HouseholdPlanApplicationService;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.ItemAmount;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Common setup of the controller tests: a mocked application service and requests sent on behalf of a
 * signed-in user ({@link #OWNER}) unless stated otherwise; reading the session token from its cookie is
 * covered by {@code HouseholdPlanIntegrationTest}.
 */
@WebMvcTest(HouseholdPlanController.class)
@Import(SecurityConfig.class)
abstract class HouseholdPlanControllerTestSupport {

    protected static final OwnerId OWNER = OwnerId.of(UUID.randomUUID());
    protected static final HouseholdPlanId PLAN_ID = HouseholdPlanId.generate();
    protected static final UUID PLAN = PLAN_ID.value();
    protected static final HouseholdPlanOverview EMPTY_OVERVIEW = HouseholdPlanOverview.of(PLAN_ID, Year.of(2026), List.of(), Map.of());

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected MockMvc anonymous;

    @MockitoBean
    protected HouseholdPlanApplicationService plans;

    protected MockMvc mockMvc;

    @BeforeEach
    void signIn() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .defaultRequest(get("/").with(jwt().jwt(token -> token.subject(OWNER.value().toString()))))
                .build();
        when(plans.addIncome(any())).thenReturn(new AddedBudgetItem(BudgetItemId.generate(), EMPTY_OVERVIEW));
        when(plans.addExpense(any())).thenReturn(new AddedBudgetItem(BudgetItemId.generate(), EMPTY_OVERVIEW));
        when(plans.changeItem(any())).thenReturn(EMPTY_OVERVIEW);
        when(plans.removeItem(any())).thenReturn(EMPTY_OVERVIEW);
        when(plans.markItemImportant(any())).thenReturn(EMPTY_OVERVIEW);
        when(plans.changeSavingPotential(any())).thenReturn(EMPTY_OVERVIEW);
    }

    protected static ItemAmount yearly(String value) {
        return ItemAmount.yearly(Money.of(new BigDecimal(value)));
    }
}
