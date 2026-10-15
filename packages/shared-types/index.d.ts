/**
 * Types of the REST contracts shared between the apps. Each contract is an OpenAPI
 * document in openapi/, kept in sync with its server by that server's OpenApiContractTest;
 * the types in generated/ are derived from them by `npm run generate`.
 */
import type { components as PlansComponents } from './generated/plans';
import type { components as UsersComponents } from './generated/users';

/** Schemas of the household plans API (apps/backend, /api/plans). */
export type PlansSchemas = PlansComponents['schemas'];

/** Schemas of the users API (apps/users, /api/auth). */
export type UsersSchemas = UsersComponents['schemas'];
