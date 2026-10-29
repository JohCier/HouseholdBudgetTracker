package de.jcss.hbt.infrastructure.web;

import de.jcss.hbt.application.networth.CopiedNetWorthStatement;
import de.jcss.hbt.application.networth.NetWorthApplicationService;
import de.jcss.hbt.domain.exception.NetWorthEntryNotFoundException;
import de.jcss.hbt.domain.exception.NetWorthStatementAlreadyExistsException;
import de.jcss.hbt.domain.exception.NetWorthStatementNotFoundException;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.networth.command.AddNetWorthEntryCommand;
import de.jcss.hbt.domain.networth.command.CopyNetWorthStatementCommand;
import de.jcss.hbt.domain.networth.overview.NetWorthHistory;
import de.jcss.hbt.domain.networth.overview.NetWorthOverview;
import de.jcss.hbt.domain.networth.overview.NetWorthOverviewEntry;
import de.jcss.hbt.domain.networth.overview.NetWorthTotals;
import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryId;
import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryName;
import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryType;
import de.jcss.hbt.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The REST API of assets and liabilities (VS-1 to VS-18), with a mocked application service. */
@WebMvcTest(NetWorthController.class)
@Import(SecurityConfig.class)
class NetWorthControllerTest {

    private static final OwnerId OWNER = OwnerId.of(UUID.randomUUID());
    private static final NetWorthOverview OVERVIEW_2026 = NetWorthOverview.of(Year.of(2026), List.of(
                    new NetWorthOverviewEntry(NetWorthEntryId.generate(), NetWorthEntryType.ASSET,
                            NetWorthEntryName.of("Girokonto"), money("21700.00")),
                    new NetWorthOverviewEntry(NetWorthEntryId.generate(), NetWorthEntryType.LIABILITY,
                            NetWorthEntryName.of("Autokredit"), money("7800.00"))),
            new NetWorthTotals(Year.of(2025), money("20100.00"), money("9600.00")));

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MockMvc anonymous;

    @MockitoBean
    private NetWorthApplicationService netWorth;

    private MockMvc mockMvc;

    @BeforeEach
    void signIn() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .defaultRequest(get("/").with(jwt().jwt(token -> token.subject(OWNER.value().toString()))))
                .build();
        when(netWorth.addEntry(any())).thenReturn(OVERVIEW_2026);
        when(netWorth.changeEntry(any())).thenReturn(OVERVIEW_2026);
        when(netWorth.removeEntry(any())).thenReturn(OVERVIEW_2026);
    }

    /** VS-6, VS-8, VS-12 */
    @Test
    void overviewListsAssetsAndLiabilitiesWithSumsAndChange() throws Exception {
        when(netWorth.getOverview(OWNER, Year.of(2026))).thenReturn(OVERVIEW_2026);

        mockMvc.perform(get("/api/net-worth/2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.assets[0].name").value("Girokonto"))
                .andExpect(jsonPath("$.assets[0].type").value("ASSET"))
                .andExpect(jsonPath("$.liabilities[0].amount").value("7800.00"))
                .andExpect(jsonPath("$.totalAssets").value("21700.00"))
                .andExpect(jsonPath("$.totalLiabilities").value("7800.00"))
                .andExpect(jsonPath("$.netWorth").value("13900.00"))
                .andExpect(jsonPath("$.overindebted").value(false))
                .andExpect(jsonPath("$.change.amount").value("3400.00"))
                .andExpect(jsonPath("$.change.percentage").value(32.38));
    }

    /** VS-18 */
    @Test
    void historyListsTheYears() throws Exception {
        when(netWorth.getHistory(OWNER)).thenReturn(NetWorthHistory.of(List.of(
                new NetWorthTotals(Year.of(2025), money("20100.00"), money("9600.00")),
                new NetWorthTotals(Year.of(2026), money("21700.00"), money("7800.00")))));

        mockMvc.perform(get("/api/net-worth/years"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.years[0].year").value(2026))
                .andExpect(jsonPath("$.years[0].netWorth").value("13900.00"))
                .andExpect(jsonPath("$.years[0].change.amount").value("3400.00"))
                .andExpect(jsonPath("$.years[1].change").doesNotExist());
    }

    /** VS-1, VS-5 */
    @Test
    void addEntryAnswersWithTheOverviewOfTheYear() throws Exception {
        mockMvc.perform(post("/api/net-worth/2026/entries").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"LIABILITY","name":"Autokredit","amount":"7800.00"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.netWorth").value("13900.00"));

        verify(netWorth).addEntry(new AddNetWorthEntryCommand(OWNER, Year.of(2026), NetWorthEntryType.LIABILITY,
                NetWorthEntryName.of("Autokredit"), money("7800.00")));
    }

    /** VS-1, VS-2 */
    @Test
    void rejectsInvalidEntries() throws Exception {
        for (String json : List.of(
                """
                        {"type":"ASSET","name":"Girokonto","amount":"0.00"}""",
                """
                        {"type":"ASSET","name":" ","amount":"1.00"}""",
                """
                        {"name":"Girokonto","amount":"1.00"}""",
                """
                        {"type":"ASSET","name":"Girokonto","amount":"1.001"}""")) {
            mockMvc.perform(post("/api/net-worth/2026/entries").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("urn:hbt:invalid-input"));
        }
        mockMvc.perform(post("/api/net-worth/2201/entries").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"ASSET","name":"Girokonto","amount":"1.00"}"""))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/net-worth/1899")).andExpect(status().isBadRequest());
        verifyNoInteractions(netWorth);
    }

    /** VS-3 */
    @Test
    void entriesOfOthersAreNotFound() throws Exception {
        when(netWorth.changeEntry(any())).thenThrow(new NetWorthStatementNotFoundException(Year.of(2026)));
        when(netWorth.removeEntry(any())).thenThrow(new NetWorthEntryNotFoundException(NetWorthEntryId.generate()));

        mockMvc.perform(put("/api/net-worth/2026/entries/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"ASSET","name":"Girokonto","amount":"1.00"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:hbt:net-worth-not-found"));
        mockMvc.perform(delete("/api/net-worth/2026/entries/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:hbt:net-worth-entry-not-found"));
    }

    /** VS-15, VS-16 */
    @Test
    void copyAnswers201ForANewYear200AfterOverwritingAnd409WithoutPermission() throws Exception {
        CopyNetWorthStatementCommand intoNewYear = new CopyNetWorthStatementCommand(OWNER, Year.of(2026),
                Year.of(2027), false);
        when(netWorth.copyStatement(intoNewYear)).thenReturn(new CopiedNetWorthStatement(false, OVERVIEW_2026));
        when(netWorth.copyStatement(new CopyNetWorthStatementCommand(OWNER, Year.of(2026), Year.of(2028), true)))
                .thenReturn(new CopiedNetWorthStatement(true, OVERVIEW_2026));
        when(netWorth.copyStatement(new CopyNetWorthStatementCommand(OWNER, Year.of(2026), Year.of(2029), false)))
                .thenThrow(new NetWorthStatementAlreadyExistsException(Year.of(2029)));

        mockMvc.perform(post("/api/net-worth/2026/copies").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetYear":2027}"""))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/net-worth/2026/copies").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetYear":2028,"overwrite":true}"""))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/net-worth/2026/copies").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetYear":2029,"overwrite":false}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:hbt:net-worth-already-exists"));
        mockMvc.perform(post("/api/net-worth/2026/copies").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetYear":2201}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresASession() throws Exception {
        anonymous.perform(get("/api/net-worth/years")).andExpect(status().isUnauthorized());
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
