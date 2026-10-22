import { expect, Page, test } from "@playwright/test";
import { login, logout, PASSWORD, register } from "./support";

const NEW_PASSWORD = "neues-geheim-1";

async function changePassword(
  page: Page,
  currentPassword: string,
  newPassword: string,
  repeat = newPassword,
): Promise<void> {
  await page.getByRole("link", { name: "Passwort ändern" }).click();
  await expect(page).toHaveURL("/passwort");
  await page.getByLabel("Aktuelles Passwort").fill(currentPassword);
  await page.getByLabel("Neues Passwort", { exact: true }).fill(newPassword);
  await page.getByLabel("Neues Passwort wiederholen").fill(repeat);
  await page.getByRole("button", { name: "Passwort ändern" }).click();
}

// docs/specs/passwort-aendern.md
test.describe("Passwort ändern", () => {
  test("nach der Änderung gilt nur noch das neue Passwort, die Sitzung bleibt (PA-1, PA-7, PA-8)", async ({
    page,
  }) => {
    const email = await register(page);

    await changePassword(page, PASSWORD, NEW_PASSWORD);

    await expect(page.getByText("Passwort geändert.")).toBeVisible();
    await expect(page).toHaveURL("/");
    await expect(page.getByText(email)).toBeVisible();

    await logout(page);
    await login(page, email);
    await expect(page.getByRole("alert")).toHaveText(
      "E-Mail-Adresse oder Passwort ist falsch.",
    );
    await login(page, email, NEW_PASSWORD);
    await expect(page).toHaveURL("/");
  });

  test("mit falschem aktuellen Passwort bleibt das alte Passwort gültig (PA-2)", async ({
    page,
  }) => {
    const email = await register(page);

    await changePassword(page, "falsches-passwort", NEW_PASSWORD);

    await expect(page.getByRole("alert")).toHaveText(
      "Das aktuelle Passwort ist falsch.",
    );
    await expect(page).toHaveURL("/passwort");
    await logout(page);
    await login(page, email);
    await expect(page).toHaveURL("/");
  });

  test("ein zu kurzes oder nicht wiederholtes neues Passwort wird nicht gesendet (PA-3, PA-5)", async ({
    page,
  }) => {
    const requests: string[] = [];
    page.on("request", (request) => requests.push(request.url()));
    await register(page);

    await changePassword(page, PASSWORD, "1234567");
    await expect(page.getByText("Mindestens 8 Zeichen.")).toBeVisible();

    await page.getByLabel("Neues Passwort", { exact: true }).fill(NEW_PASSWORD);
    await page
      .getByLabel("Neues Passwort wiederholen")
      .fill(`${NEW_PASSWORD}x`);
    await page.getByRole("button", { name: "Passwort ändern" }).click();
    await expect(
      page.getByText("Die Passwörter stimmen nicht überein."),
    ).toBeVisible();

    expect(
      requests.filter((url) => url.includes("/api/auth/password")),
    ).toEqual([]);
  });

  test("das neue Passwort muss sich vom aktuellen unterscheiden (PA-4)", async ({
    page,
  }) => {
    await register(page);

    await changePassword(page, PASSWORD, PASSWORD);

    await expect(page.getByRole("alert")).toHaveText(
      "Das neue Passwort muss sich vom aktuellen unterscheiden.",
    );
  });

  test("nach 5 falschen aktuellen Passwörtern ist auch die Anmeldung gesperrt (PA-6)", async ({
    page,
  }) => {
    const email = await register(page);
    await changePassword(page, "falsches-passwort", NEW_PASSWORD);
    for (let attempt = 1; attempt < 5; attempt++) {
      await expect(page.getByRole("alert")).toHaveText(
        "Das aktuelle Passwort ist falsch.",
      );
      await page.getByRole("button", { name: "Passwort ändern" }).click();
    }
    await expect(page.getByRole("alert")).toHaveText(
      "Das aktuelle Passwort ist falsch.",
    );

    await page.getByLabel("Aktuelles Passwort").fill(PASSWORD);
    await page.getByRole("button", { name: "Passwort ändern" }).click();
    await expect(page.getByRole("alert")).toHaveText(
      "Zu viele Anmeldeversuche. Bitte versuche es in 15 Minuten erneut.",
    );

    await logout(page);
    await login(page, email);
    await expect(page.getByRole("alert")).toHaveText(
      "Zu viele Anmeldeversuche. Bitte versuche es in 15 Minuten erneut.",
    );
  });

  test("Abbrechen führt ohne Änderung zurück (PA-8)", async ({ page }) => {
    await register(page);
    await page.getByRole("link", { name: "Passwort ändern" }).click();

    await page.getByRole("link", { name: "Abbrechen" }).click();

    await expect(page).toHaveURL("/");
  });
});
