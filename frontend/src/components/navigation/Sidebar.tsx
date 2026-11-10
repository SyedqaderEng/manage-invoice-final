import { FileText, LayoutDashboard, LogOut, Settings, ScanLine, Store, Tags } from 'lucide-react'

interface SidebarProps {
  onLogout: () => void
  mobileOpen: boolean
  onClose: () => void
  activeSection?: string
  onNavigate: (section: string) => void
}

export function Sidebar(props: SidebarProps) {
  return (
    <aside className={`sidebar ${props.mobileOpen ? 'mobile-open' : ''}`}>
      <a className="brand" href="/app" onClick={(event) => { event.preventDefault(); props.onNavigate('dashboard') }}>
        <span className="brand-mark"><ScanLine size={18} /></span><span>Ledgerly</span>
      </a>
      <span className="sidebar-caption">PERSONAL FINANCE</span>
      <nav className="sidebar-nav" aria-label="Primary navigation">
        <button className={`nav-item ${props.activeSection === 'dashboard' ? 'active' : ''}`} onClick={() => { props.onNavigate('dashboard'); props.onClose() }}>
          <LayoutDashboard size={18} aria-hidden="true" /><span>Overview</span>
        </button>
        <button className={`nav-item ${props.activeSection === 'documents' ? 'active' : ''}`} onClick={() => { props.onNavigate('documents'); props.onClose() }}>
          <FileText size={18} aria-hidden="true" /><span>Documents</span>
        </button>
        <button className={`nav-item ${props.activeSection === 'vendors' ? 'active' : ''}`} onClick={() => { props.onNavigate('vendors'); props.onClose() }}>
          <Store size={18} aria-hidden="true" /><span>Vendors</span>
        </button>
        <button className={`nav-item ${props.activeSection === 'categories' ? 'active' : ''}`} onClick={() => { props.onNavigate('categories'); props.onClose() }}>
          <Tags size={18} aria-hidden="true" /><span>Categories</span>
        </button>
      </nav>
      <div className="sidebar-bottom">
        <span className="sidebar-caption">YOUR ACCOUNT</span>
        <nav className="sidebar-nav" aria-label="Account navigation">
          <button className={`nav-item ${props.activeSection === 'settings' ? 'active' : ''}`} onClick={() => { props.onNavigate('settings'); props.onClose() }}>
            <Settings size={18} aria-hidden="true" /><span>Settings</span>
          </button>
          <button className="nav-item nav-button" onClick={props.onLogout}><LogOut size={18} aria-hidden="true" /><span>Sign out</span></button>
        </nav>
        <div className="sidebar-private"><span><ScanLine size={15} /></span><p><strong>Your space</strong><small>Private by design</small></p></div>
      </div>
    </aside>
  )
}
