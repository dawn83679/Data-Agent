import http from '../lib/http';
import { UpdateUserRequest } from '../types/auth';

export const userService = {
    /**
     * Update current user profile
     */
    updateProfile: async (data: UpdateUserRequest): Promise<void> => {
        await http.put<void>('/user/me', data);
    },
};
