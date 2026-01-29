import { Navigate, type RouteObject } from "react-router-dom";
import { RouteGuard } from "./components/auth/RouteGuard";
import Home from "./pages/Home";
import Settings from "./pages/Settings";
import Profile from "./pages/Profile";
import Sessions from "./pages/Sessions";

interface RouterConfig {
    path?: string;
    element?: React.ReactNode;
    children?: RouterConfig[];
    index?: boolean;
    /**
     * 是否需要登录
     */
    requiresAuth?: boolean;
}

const routes: RouterConfig[] = [
    {
        path: "/",
        element: <Home />,
    },
    {
        path: "/settings",
        element: <Settings />,
        requiresAuth: true,
        children: [
            { index: true, element: <Navigate to="/settings/profile" replace /> },
            { path: "profile", element: <Profile />, requiresAuth: true },
            { path: "sessions", element: <Sessions />, requiresAuth: true },
        ],
    },
];

// 递归包装需要登录的路由
const applyRouteGuard = (configs: RouterConfig[]): RouterConfig[] => {
    return configs.map((route) => {
        const guarded: RouterConfig = {
            ...route,
            element: route.requiresAuth ? (
                <RouteGuard>{route.element}</RouteGuard>
            ) : (
                route.element
            ),
        };

        if (route.children && route.children.length > 0) {
            guarded.children = applyRouteGuard(route.children);
        }

        return guarded;
    });
};

export const routerConfig: RouteObject[] = applyRouteGuard(routes) as RouteObject[];
