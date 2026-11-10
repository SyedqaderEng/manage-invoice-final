import { Menu } from 'lucide-react'
import { Avatar } from '../ui/Avatar'

interface TopBarProps {
  userName: string
  onMenu: () => void
}

export function TopBar({ userName, onMenu }: TopBarProps) {
  return (
    <header className="topbar">
      <button className="icon-button mobile-menu" aria-label="Open navigation" onClick={onMenu}><Menu size={20} /></button>
      <span className="topbar-context"><span className="context-dot" />PERSONAL SPACE</span>
      <div className="topbar-actions"><span className="user-identity"><strong>{userName}</strong><small>Personal account</small></span><Avatar name={userName} /></div>
    </header>
  )
}
