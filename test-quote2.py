from fpdf import FPDF

class QuotePDF(FPDF):
    def header(self):
        self.set_font("Helvetica", "B", 18)
        self.set_text_color(20, 120, 60)
        self.cell(0, 12, "MIDLANDS STEEL MERCHANTS PLC", ln=True)
        self.set_font("Helvetica", "", 9)
        self.set_text_color(80, 80, 80)
        self.cell(0, 5, "Parkway Business Park, Unit 14, Birmingham, B24 9QR", ln=True)
        self.cell(0, 5, "Tel: 0121 456 9900  |  quotes@midlandssteelmerchants.co.uk  |  VAT: GB 198 4453 22", ln=True)
        self.ln(3)
        self.set_draw_color(20, 120, 60)
        self.set_line_width(0.5)
        self.line(10, self.get_y(), 200, self.get_y())
        self.ln(4)

    def footer(self):
        self.set_y(-15)
        self.set_font("Helvetica", "I", 8)
        self.set_text_color(150, 150, 150)
        self.cell(0, 10, f"Page {self.page_no()} | Midlands Steel Merchants PLC | E&OE - All prices exclude VAT unless stated", align="C")


pdf = QuotePDF()
pdf.add_page()

# Quote header
pdf.set_font("Helvetica", "B", 13)
pdf.set_text_color(20, 120, 60)
pdf.cell(0, 8, "PRICE QUOTATION", ln=True)

pdf.set_font("Helvetica", "", 9)
pdf.set_text_color(50, 50, 50)

col_w = 90
pdf.set_font("Helvetica", "B", 9)
pdf.cell(col_w, 6, "Quote Reference:", border=0)
pdf.cell(col_w, 6, "Bill To:", border=0, ln=True)

pdf.set_font("Helvetica", "", 9)
pdf.cell(col_w, 5, "MSM/Q/2024/3291", border=0)
pdf.cell(col_w, 5, "Apex Construction Ltd", border=0, ln=True)
pdf.cell(col_w, 5, "", border=0)
pdf.cell(col_w, 5, "14 Meridian Way, London, EC2A 4NB", border=0, ln=True)

pdf.set_font("Helvetica", "B", 9)
pdf.cell(col_w, 6, "Quote Date:", border=0)
pdf.cell(col_w, 6, "Attention:", border=0, ln=True)

pdf.set_font("Helvetica", "", 9)
pdf.cell(col_w, 5, "04 April 2024", border=0)
pdf.cell(col_w, 5, "James Hargreaves  |  procurement@apexconstruction.co.uk", border=0, ln=True)

pdf.set_font("Helvetica", "B", 9)
pdf.cell(col_w, 6, "Valid Until:", border=0)
pdf.cell(col_w, 6, "", border=0, ln=True)

pdf.set_font("Helvetica", "", 9)
pdf.cell(col_w, 5, "04 May 2024", border=0)
pdf.ln(6)

# Project ref banner
pdf.set_fill_color(235, 250, 240)
pdf.set_font("Helvetica", "B", 9)
pdf.cell(0, 7, "  Project Reference: Apex - Wembley Site Phase 3 Structural Steel Supply", fill=True, ln=True)
pdf.ln(4)

# Table header
pdf.set_fill_color(20, 120, 60)
pdf.set_text_color(255, 255, 255)
pdf.set_font("Helvetica", "B", 9)
col_widths = [10, 80, 18, 18, 25, 25]
headers = ["#", "Description", "Qty", "Unit", "Unit Price", "Line Total"]
for w, h in zip(col_widths, headers):
    pdf.cell(w, 8, h, border=0, fill=True, align="C" if h != "Description" else "L")
pdf.ln()

# Line items — same spec, slightly different prices and a couple of variations
items = [
    (1,  "UC 203x203x60 Universal Column S355 JR",             "48",  "length",   "£192.00",  "£9,216.00"),
    (2,  "UB 457x191x82 Universal Beam S355 JR",               "120", "length",   "£228.50",  "£27,420.00"),
    (3,  "PFC 200x75 Parallel Flange Channel S275",             "36",  "length",   "£101.20",  "£3,643.20"),
    (4,  "Equal Angle 100x100x10 (S275)",                       "60",  "length",   "£46.00",   "£2,760.00"),
    (5,  "Base Plate 300x300x20 pre-drilled (batch of 4)",      "12",  "batch",    "£118.00",  "£1,416.00"),
    (6,  "M24 HDG Bolt Sets Grade 8.8 (50-piece box)",          "12",  "box",      "£71.50",   "£858.00"),
    (7,  "Gusset Plate 200x200x10 flame-cut finish",            "96",  "each",     "£13.90",   "£1,334.40"),
    (8,  "Structural Steel Sheet 3mm 2440x1220 S275",           "24",  "sheet",    "£108.50",  "£2,604.00"),
    (9,  "Blast clean & zinc phosphate primer (full lot)",       "1",   "lot",      "£2,100.00","£2,100.00"),
    (10, "Delivery - 2x artic loads, Wembley HA9 (HIAB avail)", "2",   "delivery", "£390.00",  "£780.00"),
]

pdf.set_text_color(30, 30, 30)
pdf.set_font("Helvetica", "", 9)
for i, row in enumerate(items):
    fill = i % 2 == 0
    pdf.set_fill_color(242, 252, 245) if fill else pdf.set_fill_color(255, 255, 255)
    vals = [str(row[0]), row[1], row[2], row[3], row[4], row[5]]
    for w, v in zip(col_widths, vals):
        pdf.cell(w, 7, v, border=0, fill=True, align="C" if v not in (row[1],) else "L")
    pdf.ln()

pdf.ln(4)

# Totals
x_label = 130
x_value = 165
w_label = 35
w_value = 30

def total_row(label, value, bold=False, bg=None):
    pdf.set_font("Helvetica", "B" if bold else "", 10 if bold else 9)
    pdf.set_x(x_label)
    if bg:
        pdf.set_fill_color(*bg)
        pdf.cell(w_label, 7, label, border=0, fill=True, align="R")
        pdf.cell(w_value, 7, value, border=0, fill=True, align="R")
    else:
        pdf.cell(w_label, 7, label, border=0, align="R")
        pdf.cell(w_value, 7, value, border=0, align="R")
    pdf.ln()

total_row("Subtotal:", "£52,131.60")
total_row("VAT (20%):", "£10,426.32")
pdf.set_draw_color(20, 120, 60)
pdf.set_x(x_label)
pdf.line(x_label, pdf.get_y(), x_label + w_label + w_value, pdf.get_y())
pdf.ln(1)
total_row("TOTAL (inc. VAT):", "£62,557.92", bold=True, bg=(220, 245, 230))
pdf.ln(6)

# Terms
pdf.set_font("Helvetica", "B", 9)
pdf.set_text_color(20, 120, 60)
pdf.cell(0, 6, "Terms & Conditions", ln=True)
pdf.set_font("Helvetica", "", 8)
pdf.set_text_color(70, 70, 70)
terms = [
    "Payment Terms:  45 days net from invoice date. No early payment discount offered.",
    "Lead Time:  UC/UB sections ex-stock within 3 working days. Fabricated/cut items 8-10 working days.",
    "Delivery:  Price includes 2 x articulated lorry deliveries. HIAB crane offload available at £95/hr if required.",
    "Validity:  Prices held firm for 30 days. Subject to mill price movements beyond this date.",
    "Certifications:  All material supplied with EN 10025 mill certificates. CARES approved stockholder.",
]
for t in terms:
    pdf.cell(0, 5, t, ln=True)

pdf.ln(4)
pdf.set_font("Helvetica", "I", 8)
pdf.set_text_color(120, 120, 120)
pdf.cell(0, 5, "Prepared by: K. Patel  |  Commercial Manager  |  Midlands Steel Merchants PLC", ln=True)

output_path = "/Users/shoaiblatif/Documents/procurement-ai/test-quote-midlands.pdf"
pdf.output(output_path)
print(f"PDF written to: {output_path}")
