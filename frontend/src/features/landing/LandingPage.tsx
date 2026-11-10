import { ArrowDown, ArrowRight, Check, FileImage, FileText, ScanLine, ShieldCheck, Sparkles } from 'lucide-react'
import { Button } from '../../components/ui/Button'

interface LandingPageProps {
  onNavigate: (path: string) => void
}

const plans = [
  { name: 'Free', price: '$0', period: 'forever', description: 'A simpler way to keep receipts in one place.', features: ['15 processed documents each month', 'AI-assisted extraction', 'Original document storage', 'Digital invoice viewing', 'Basic search and CSV export'] },
  { name: 'Pro', price: '$10', period: 'per month', description: 'More insight and room to organize everything.', features: ['500 processed documents each month', 'Everything in Free', 'Advanced analytics and history', 'CSV, XML, and PDF exports', 'Bulk downloads and priority processing'], featured: true },
]

export function LandingPage({ onNavigate }: LandingPageProps) {
  return (
    <main className="landing-page">
      <header className="landing-nav-wrap">
        <nav className="landing-nav" aria-label="Main navigation">
          <a className="landing-brand" href="/" onClick={(event) => { event.preventDefault(); onNavigate('/') }}>
            <span className="brand-mark"><ScanLine size={19} /></span><span>Ledgerly</span>
          </a>
          <div className="landing-nav-links">
            <a href="#how-it-works">How it works</a>
            <a href="#features">Features</a>
            <a href="#pricing">Pricing</a>
          </div>
          <div className="landing-actions">
            <Button variant="ghost" onClick={() => onNavigate('/login')}>Sign in</Button>
            <Button onClick={() => onNavigate('/register')}>Start for free <ArrowRight size={16} /></Button>
          </div>
        </nav>
      </header>

      <section className="landing-hero">
        <div className="landing-copy">
          <div className="landing-eyebrow"><span className="eyebrow-dot" />YOUR MONEY, IN FOCUS</div>
          <h1>Every receipt.<br />Every invoice.<br /><span>One organized place.</span></h1>
          <p>Capture your receipts, organize your invoices, and understand exactly where your money goes — automatically.</p>
          <div className="landing-hero-actions">
            <Button onClick={() => onNavigate('/register')}>Start for free <ArrowRight size={17} /></Button>
            <a href="#how-it-works">Explore features <ArrowDown size={15} /></a>
          </div>
          <div className="hero-assurance"><ShieldCheck size={16} /><span>Your documents stay private to you.</span></div>
        </div>

        <div className="hero-visual" aria-label="Illustration of a receipt becoming an organized digital record">
          <div className="hero-orbit orbit-one" /><div className="hero-orbit orbit-two" />
          <div className="receipt-card">
            <div className="receipt-header"><span className="receipt-logo">w</span><div><strong>Walmart</strong><small>Everyday essentials</small></div><span className="receipt-check"><Check size={15} /></span></div>
            <div className="receipt-date">RECEIPT <span>•</span> OCT 24, 2026</div>
            <div className="receipt-line"><span>Organic bananas</span><strong>$2.48</strong></div>
            <div className="receipt-line"><span>Whole milk, 1 gal</span><strong>$3.96</strong></div>
            <div className="receipt-line"><span>Household supplies</span><strong>$12.47</strong></div>
            <div className="receipt-divider" />
            <div className="receipt-total"><span>Total</span><strong>$18.91</strong></div>
            <div className="receipt-ai"><Sparkles size={14} /><span>Details organized for you</span></div>
          </div>
          <div className="floating-tag tag-scan"><span className="tag-icon violet"><ScanLine size={17} /></span><span><strong>Receipt scanned</strong><small>Ready to review</small></span><Check size={16} className="tag-success" /></div>
          <div className="floating-tag tag-category"><span className="tag-icon blue"><FileText size={17} /></span><span><strong>Groceries</strong><small>Category matched</small></span></div>
          <div className="hero-visual-caption"><span className="caption-line" /> FROM PAPER TO CLARITY</div>
        </div>
      </section>

      <section className="trust-strip" aria-label="Product benefits">
        <span>MADE FOR REAL LIFE</span><i /><strong>Receipts</strong><i /><strong>Invoices</strong><i /><strong>Medical bills</strong><i /><strong>Service work</strong><i /><strong>All in one place</strong>
      </section>

      <section className="how-section section-wrap" id="how-it-works">
        <div className="section-intro"><div className="section-kicker">A LITTLE LESS PAPERWORK</div><h2>From crumpled receipt<br />to clear picture.</h2><p>No spreadsheets. No shoeboxes. Just a calmer way to keep track of what you spend.</p></div>
        <div className="steps-grid">
          <article className="step-card"><span className="step-number">01</span><span className="step-icon"><FileImage size={20} /></span><h3>Capture anything</h3><p>Snap a receipt, add a photo, or drop in an invoice PDF. Your phone camera works too.</p><div className="step-detail">PHOTO <i /> PDF <i /> RECEIPT</div></article>
          <article className="step-card"><span className="step-number">02</span><span className="step-icon"><Sparkles size={20} /></span><h3>Let the details surface</h3><p>Vendor, date, totals, and line items are organized so you can check everything at a glance.</p><div className="step-detail">CLEAR DETAILS, LESS TYPING</div></article>
          <article className="step-card"><span className="step-number">03</span><span className="step-icon"><ScanLine size={20} /></span><h3>Find your whole picture</h3><p>Keep the original close, search what matters, and see where your money goes over time.</p><div className="step-detail">YOUR RECORDS, YOUR WAY</div></article>
        </div>
      </section>

      <section className="feature-section" id="features">
        <div className="feature-layout section-wrap">
          <div className="feature-copy"><div className="section-kicker">THOUGHTFULLY ORGANIZED</div><h2>Keep the original.<br /><span>Know the details.</span></h2><p>Your photo or PDF stays connected to its digital record. Review the numbers, make a correction, and always get back to the source.</p><ul><li><Check size={16} /> Original document remains yours</li><li><Check size={16} /> Review extracted details before saving</li><li><Check size={16} /> Keep vendors and spending organized</li></ul><Button variant="secondary" onClick={() => onNavigate('/register')}>See how it works <ArrowRight size={16} /></Button></div>
          <div className="record-visual">
            <div className="source-paper"><div className="paper-top"><span className="paper-mark">W</span><div><strong>Walmart</strong><small>Store receipt</small></div></div><div className="paper-squiggle" /><div className="paper-squiggle short" /><div className="paper-items"><span /><span /><span /><span /></div><div className="paper-total"><span>TOTAL</span><strong>$18.91</strong></div><div className="paper-label"><FileImage size={14} /> Original photo</div></div>
            <div className="record-connector"><span /><span /><span /></div>
            <div className="digital-record"><div className="digital-record-top"><span><Sparkles size={14} /> DIGITAL RECORD</span><span className="record-verified"><Check size={13} /> Reviewed</span></div><h3>Walmart</h3><p>October 24, 2026</p><div className="record-item"><span>Organic bananas</span><strong>$2.48</strong></div><div className="record-item"><span>Whole milk, 1 gal</span><strong>$3.96</strong></div><div className="record-item"><span>Household supplies</span><strong>$12.47</strong></div><div className="record-total"><span>Total</span><strong>$18.91</strong></div><div className="record-category">Groceries <span>•</span> Everyday essentials</div></div>
          </div>
        </div>
      </section>

      <section className="document-types section-wrap">
        <div><div className="section-kicker">NOT JUST SHOPPING</div><h2>Life doesn’t come<br />with one kind of receipt.</h2></div>
        <p>From weekly groceries to a hospital bill or the contractor who fixed the roof, keep the paperwork that matters together.</p>
        <div className="document-type-list"><span>RETAIL RECEIPTS</span><span>MEDICAL BILLS</span><span>CONTRACTOR INVOICES</span><span>SERVICE WORK</span><span>PDFS & PHOTOS</span></div>
      </section>

      <section className="pricing-section" id="pricing">
        <div className="section-wrap pricing-wrap">
          <div className="section-intro pricing-intro"><div className="section-kicker">SIMPLE, HONEST PRICING</div><h2>Start free.<br />Grow when you’re ready.</h2><p>Keep the essentials for free. Upgrade when you want more room and deeper insight.</p></div>
          <div className="pricing-cards">{plans.map((plan) => (
            <article key={plan.name} className={`price-card ${plan.featured ? 'featured' : ''}`}>
              {plan.featured ? <div className="popular-label">A LITTLE MORE ROOM</div> : null}
              <h3>{plan.name}</h3><p>{plan.description}</p><div className="price-amount">{plan.price}<span> / {plan.period}</span></div>{plan.name === 'Pro' ? <div className="annual-note">Or $99 per year</div> : <div className="annual-note">&nbsp;</div>}
              <Button variant={plan.featured ? 'primary' : 'secondary'} onClick={() => onNavigate('/register')}>Start with {plan.name} <ArrowRight size={16} /></Button>
              <div className="price-divider" /><ul>{plan.features.map((feature) => <li key={feature}><Check size={15} />{feature}</li>)}</ul>
            </article>
          ))}</div>
        </div>
      </section>

      <section className="faq-section section-wrap">
        <div className="section-intro"><div className="section-kicker">GOOD QUESTIONS</div><h2>A few things<br />you might wonder.</h2></div>
        <div className="faq-list">
          <details><summary>What kinds of documents can I save?<span>+</span></summary><p>Receipts and invoices as photos or PDFs, including retail purchases, medical bills, contractor work, and other service documents.</p></details>
          <details><summary>Can I keep my original receipt?<span>+</span></summary><p>Yes. The original image or PDF stays connected to its organized digital record, so you can return to the source whenever you need it.</p></details>
          <details><summary>Do I have to pay to get started?<span>+</span></summary><p>No. The Free plan includes up to 15 processed documents each month, with no payment details needed to start.</p></details>
          <details><summary>What happens if a detail doesn’t look right?<span>+</span></summary><p>You’ll be able to review the extracted details alongside your original document and correct them before saving.</p></details>
        </div>
      </section>

      <section className="final-cta"><div className="cta-glow" /><div className="section-kicker">A FRESH START FOR YOUR PAPERWORK</div><h2>Make room for a clearer picture.</h2><p>Start organizing the receipts and invoices you already have.</p><Button onClick={() => onNavigate('/register')}>Start for free <ArrowRight size={17} /></Button><small>No card required <span>·</span> Your documents stay private</small></section>

      <footer className="site-footer"><a className="landing-brand" href="/"><span className="brand-mark"><ScanLine size={19} /></span><span>Ledgerly</span></a><span>Less paper. More peace of mind.</span><div><a href="#features">Features</a><a href="#pricing">Pricing</a><a href="/login" onClick={(event) => { event.preventDefault(); onNavigate('/login') }}>Sign in</a></div><small>© 2026 Ledgerly. Your records stay yours.</small></footer>
    </main>
  )
}
