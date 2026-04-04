from fpdf import FPDF

class QuotePDF(FPDF):
    def header(self):
        self.set_font("Helvetica", "B", 18)
        self.set_text_color(30, 80, 160)
        self.cell(0, 12, "STEELFRAME SUPPLIES LTD", ln=True)
        self.set_font("Helvetica", "", 9)
        self.set_text_color(80, 80, 80)
        self.cell(0, 5, "Unit 7, Forge Industrial Estate, Sheffield, S9 2LR", ln=True)
        self.cell(0, 5, "Tel: 0114 278 4400  |  sales@steelframesupplies.co.uk  |  VAT: GB 234 8821 09", ln=True)
        self.ln(3)
        self.set_draw_color(30, 80, 160)
        self.set_line_width(0.5)
        self.line(10, self.get_y(), 200, self.get_y())
        self.ln(4)

    def footer(self):
        self.set_y(-15)
        self.set_font("Helvetica", "I", 8)
        self.set_text_color(150, 150, 150)
        self.cell(0, 10, f"Page {self.page_no()} | Steelframe Supplies Ltd | Quote valid for 30 days from issue date", align="C")


pdf = QuotePDF()
pdf.add_page()

# Quote header block
pdf.set_font("Helvetica", "B", 13)
pdf.set_text_color(30, 80, 160)
pdf.cell(0, 8, "QUOTATION", ln=True)

pdf.set_font("Helvetica", "", 9)
pdf.set_text_color(50, 50, 50)

# Two-column header info
col_w = 90
pdf.set_font("Helvetica", "B", 9)
pdf.cell(col_w, 6, "Quote Reference:", border=0)
pdf.cell(col_w, 6, "Bill To:", border=0, ln=True)

pdf.set_font("Helvetica", "", 9)
pdf.cell(col_w, 5, "SFS-2024-00847", border=0)
pdf.cell(col_w, 5, "Apex Construction Ltd", border=0, ln=True)
pdf.cell(col_w, 5, "", border=0)
pdf.cell(col_w, 5, "14 Meridian Way, London, EC2A 4NB", border=0, ln=True)

pdf.set_font("Helvetica", "B", 9)
pdf.cell(col_w, 6, "Quote Date:", border=0)
pdf.cell(col_w, 6, "Contact:", border=0, ln=True)

pdf.set_font("Helvetica", "", 9)
pdf.cell(col_w, 5, "03 April 2024", border=0)
pdf.cell(col_w, 5, "James Hargreaves  |  procurement@apexconstruction.co.uk", border=0, ln=True)

pdf.set_font("Helvetica", "B", 9)
pdf.cell(col_w, 6, "Valid Until:", border=0)
pdf.cell(col_w, 6, "", border=0, ln=True)

pdf.set_font("Helvetica", "", 9)
pdf.cell(col_w, 5, "03 May 2024", border=0)
pdf.ln(6)

# Project ref
pdf.set_fill_color(240, 245, 255)
pdf.set_font("Helvetica", "B", 9)
pdf.cell(0, 7, "  Project Reference: Apex - Wembley Site Phase 3 Structural Steel", fill=True, ln=True)
pdf.ln(4)

# Table header
pdf.set_fill_color(30, 80, 160)
pdf.set_text_color(255, 255, 255)
pdf.set_font("Helvetica", "B", 9)
col_widths = [10, 80, 18, 18, 25, 25]
headers = ["#", "Description", "Qty", "Unit", "Unit Price", "Line Total"]
for w, h in zip(col_widths, headers):
    pdf.cell(w, 8, h, border=0, fill=True, align="C" if h != "Description" else "L")
pdf.ln()

# Line items
items = [
    (1, "UC 203x203x60 Universal Column (Grade S355)", "48", "length", "£187.50", "£9,000.00"),
    (2, "UB 457x191x82 Universal Beam (Grade S355)", "120", "length", "£241.00", "£28,920.00"),
    (3, "PFC 200x75 Parallel Flange Channel", "36", "length", "£98.40", "£3,542.40"),
    (4, "Equal Angle 100x100x10 L-Section (S275)", "60", "length", "£44.80", "£2,688.00"),
    (5, "Base Plate 300x300x20 pre-drilled", "48", "each", "£28.50", "£1,368.00"),
    (6, "M24 Bolt Sets (HDG, Grade 8.8) Box of 50", "12", "box", "£67.20", "£806.40"),
    (7, "Gusset Plate 200x200x10 cut to size", "96", "each", "£14.75", "£1,416.00"),
    (8, "3mm Structural Steel Sheet 2440x1220", "24", "sheet", "£112.00", "£2,688.00"),
    (9, "Primer coat (grey oxide) spray application", "1", "lot", "£1,840.00", "£1,840.00"),
    (10, "Delivery to site (Wembley HA9) - 2 articulated loads", "2", "delivery", "£420.00", "£840.00"),
]

pdf.set_text_color(30, 30, 30)
pdf.set_font("Helvetica", "", 9)
for i, row in enumerate(items):
    fill = i % 2 == 0
    pdf.set_fill_color(248, 250, 255) if fill else pdf.set_fill_color(255, 255, 255)
    vals = [str(row[0]), row[1], row[2], row[3], row[4], row[5]]
    for w, v in zip(col_widths, vals):
        pdf.cell(w, 7, v, border=0, fill=True, align="C" if v not in (row[1],) else "L")
    pdf.ln()

pdf.ln(4)

# Totals block (right-aligned)
pdf.set_draw_color(200, 200, 200)
x_label = 130
x_value = 165
w_label = 35
w_value = 30

def total_row(label, value, bold=False, bg=None):
    if bold:
        pdf.set_font("Helvetica", "B", 10)
    else:
        pdf.set_font("Helvetica", "", 9)
    if bg:
        pdf.set_fill_color(*bg)
        pdf.set_x(x_label)
        pdf.cell(w_label, 7, label, border=0, fill=True, align="R")
        pdf.cell(w_value, 7, value, border=0, fill=True, align="R")
    else:
        pdf.set_x(x_label)
        pdf.cell(w_label, 7, label, border=0, align="R")
        pdf.cell(w_value, 7, value, border=0, align="R")
    pdf.ln()

total_row("Subtotal:", "£53,108.80")
total_row("VAT (20%):", "£10,621.76")
pdf.set_draw_color(30, 80, 160)
pdf.set_x(x_label)
pdf.line(x_label, pdf.get_y(), x_label + w_label + w_value, pdf.get_y())
pdf.ln(1)
total_row("TOTAL (inc. VAT):", "£63,730.56", bold=True, bg=(230, 240, 255))
pdf.ln(6)

# Terms
pdf.set_font("Helvetica", "B", 9)
pdf.set_text_color(30, 80, 160)
pdf.cell(0, 6, "Terms & Conditions", ln=True)
pdf.set_font("Helvetica", "", 8)
pdf.set_text_color(70, 70, 70)
terms = [
    "Payment Terms:  30 days net from invoice date. 2% early payment discount if settled within 10 days.",
    "Lead Time:  Stock items available within 5 working days. Cut/fabricated items 10-12 working days.",
    "Delivery:  Included for orders over £5,000 within 50 miles of Sheffield. Site access must be confirmed 48h prior.",
    "Validity:  This quotation is valid for 30 days from the date of issue. Prices subject to steel index fluctuations.",
    "Returns:  Cut-to-size or fabricated items are non-returnable. Standard stock items subject to 15% restocking fee.",
]
for t in terms:
    pdf.cell(0, 5, t, ln=True)

pdf.ln(4)
pdf.set_font("Helvetica", "I", 8)
pdf.set_text_color(120, 120, 120)
pdf.cell(0, 5, "Authorised by: D. Thornton  |  Sales Director  |  Steelframe Supplies Ltd", ln=True)

output_path = "/Users/shoaiblatif/Documents/procurement-ai/test-quote-steelframe.pdf"
pdf.output(output_path)
print(f"PDF written to: {output_path}")
