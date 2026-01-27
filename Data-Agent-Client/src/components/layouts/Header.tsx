import { Button } from "../ui/Button";
import { ThemeSwitcher } from "../common/ThemeSwitcher";

interface HeaderProps {
    onLoginClick: () => void;
}

export function Header({ onLoginClick }: HeaderProps) {
    return (
        <header className="sticky top-0 z-50 w-full border-b border-border/40 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
            <div className="container flex h-14 max-w-screen-2xl items-center justify-between px-4">
                <div className="flex items-center gap-2">
                    <span className="text-xl font-bold bg-gradient-to-r from-blue-600 to-blue-400 bg-clip-text text-transparent">
                        Data Agent
                    </span>
                </div>

                <div className="flex items-center gap-4">
                    <Button variant="ghost" size="sm" onClick={onLoginClick}>
                        Login
                    </Button>
                    <ThemeSwitcher />
                </div>
            </div>
        </header>
    );
}
