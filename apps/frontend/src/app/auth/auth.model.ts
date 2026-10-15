/**
 * Client-side representation of the users REST contract (apps/users, /api/auth). The types
 * come from @hbt/shared-types, generated from packages/shared-types/openapi/users.json.
 */
import type { UsersSchemas as Schemas } from '@hbt/shared-types';

export type User = Schemas['UserResponse'];
export type LoginRequest = Schemas['LoginRequest'];
export type RegisterUserRequest = Schemas['RegisterUserRequest'];

/** Password rules of the server, checked in the forms already to give early feedback. */
export const PASSWORD_MIN_LENGTH = 8;
export const PASSWORD_MAX_LENGTH = 72;
