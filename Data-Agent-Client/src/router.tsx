import { lazy, Suspense } from "react";
import { Navigate, type RouteObject } from "react-router-dom";
import { RouteGuard } from "./components/auth/RouteGuard";

const Home = lazy(() => import("./pages/Home").then((m) => ({ default: m.default })));
const Settings = lazy(() => import("./pages/Settings").then((m) => ({ default: m.default })));
const Profile = lazy(() => import("./pages/Profile").then((m) => ({ default: m.default })));
const PasswordReset = lazy(() => import("./pages/PasswordReset").then((m) => ({ default: m.default })));
const Sessions = lazy(() => import("./pages/Sessions").then((m) => ({ default: m.default })));

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
        element: (
            <Suspense fallback={null}>
                <Home />
            </Suspense>
        ),
    },
    {
        path: "/profile",
        element: <Navigate to="/settings/profile" replace />,
    },
    {
        path: "/settings",
        element: (
            <Suspense fallback={null}>
                <Settings />
            </Suspense>
        ),
        requiresAuth: true,
        children: [
            { index: true, element: <Navigate to="/settings/profile" replace /> },
            { path: "profile", element: <Suspense fallback={null}><Profile /></Suspense>, requiresAuth: true },
            { path: "password", element: <Suspense fallback={null}><PasswordReset /></Suspense>, requiresAuth: true },
            { path: "sessions", element: <Suspense fallback={null}><Sessions /></Suspense>, requiresAuth: true },
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
