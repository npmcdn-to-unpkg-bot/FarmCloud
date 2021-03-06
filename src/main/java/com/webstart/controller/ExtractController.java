package com.webstart.controller;

import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.ByteBuffer;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.codec.Base64;
import com.webstart.DTO.ObservableMeasure;
import com.webstart.DTO.ValueTime;
import com.webstart.model.Users;
import com.webstart.repository.ObservationJpaRepository;
import com.webstart.service.ObservationProperyService;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.text.Document;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;


@Controller
@RequestMapping("extract")
public class ExtractController {

    @Autowired
    ObservationProperyService observationProperyService;

    @RequestMapping(value = "csv", params = {"id", "mydevice", "dtstart", "dtend"}, method = RequestMethod.POST)
    public void getCsv(@RequestParam("id") Long id, @RequestParam("mydevice") String mydevice, @RequestParam("dtstart") String datetimestart, @RequestParam("dtend") String datetimeend, HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/plain");
        String reportName = "Measures.csv";
        response.setHeader("Content-disposition", "attachment; filename=" + reportName);

        try {
            Users user = (Users) request.getSession().getAttribute("current_user");
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date from = dateFormat.parse(datetimestart);
            Date to = dateFormat.parse(datetimeend);
            ObservableMeasure observableMeasure = observationProperyService.getObservationData(id, user.getUser_id(), mydevice, from, to);

            ArrayList<String> rows = new ArrayList<String>();
            rows.add("Measure date & time");
            rows.add(";");
            rows.add("Measure value");
            rows.add(";");
            rows.add("Measure unit");
            rows.add("\n");

            // write table row data
            for (ValueTime valueTime : observableMeasure.getMeasuredata()) {
                rows.add(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(valueTime.getPhenomenonDateTime()));
                rows.add(";");
                rows.add(valueTime.getValue().toString());
                rows.add(";");
                rows.add(observableMeasure.getUnit());
                rows.add("\n");
            }

            Iterator<String> iter = rows.iterator();
            while (iter.hasNext()) {
                String outputString = (String) iter.next();
                response.getOutputStream().print(outputString);
            }

            response.getOutputStream().flush();

        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    @RequestMapping(value = "pdf", params = {"id", "mydevice", "dtstart", "dtend"}, method = RequestMethod.POST)
    public ResponseEntity<byte[]> getPDF(@RequestParam("id") Long id, @RequestParam("mydevice") String mydevice, @RequestParam("dtstart") String datetimestart, @RequestParam("dtend") String datetimeend, HttpServletRequest request) {
        ObservableMeasure observableMeasure = null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] contents;

        try {
            Users user = (Users) request.getSession().getAttribute("current_user");
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date from = dateFormat.parse(datetimestart);
            Date to = dateFormat.parse(datetimeend);
            observableMeasure = observationProperyService.getObservationData(id, user.getUser_id(), mydevice, from, to);
            com.itextpdf.text.Document doc = new com.itextpdf.text.Document();
            PdfWriter.getInstance(doc, byteArrayOutputStream);  // Do this BEFORE document.open()
            doc.open();
            doc = createPDF(observableMeasure, datetimestart, datetimeend, doc);  // Whatever function that you use to create your PDF
            doc.close();
            contents = byteArrayOutputStream.toByteArray();
            // generate the file
        } catch (Exception exc) {
            exc.printStackTrace();
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/pdf"));
        String filename = "measures.pdf";
        headers.setContentDispositionFormData(filename, filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        ResponseEntity<byte[]> response = new ResponseEntity<byte[]>(contents, headers, HttpStatus.OK);

        return response;
    }

    @RequestMapping(value = "xls", params = {"id", "mydevice", "dtstart", "dtend"}, method = RequestMethod.POST)
    public ResponseEntity<byte[]> getXlsx(@RequestParam("id") Long id, @RequestParam("mydevice") String mydevice, @RequestParam("dtstart") String datetimestart, @RequestParam("dtend") String datetimeend, HttpServletRequest request) {
        ObservableMeasure observableMeasure = null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] contents;

        try {
            Users user = (Users) request.getSession().getAttribute("current_user");
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date from = dateFormat.parse(datetimestart);
            Date to = dateFormat.parse(datetimeend);
            observableMeasure = observationProperyService.getObservationData(id, user.getUser_id(), mydevice, from, to);

            //Create excel document
            HSSFWorkbook wbook = createXlsx(observableMeasure);
            wbook.write(byteArrayOutputStream);
            contents = byteArrayOutputStream.toByteArray();
            // generate the file
        } catch (Exception exc) {
            exc.printStackTrace();
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String filename = "measures.xls";
        headers.setContentDispositionFormData(filename, filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        ResponseEntity<byte[]> response = new ResponseEntity<byte[]>(contents, headers, HttpStatus.OK);

        return response;
    }


    private com.itextpdf.text.Document createPDF(ObservableMeasure observableMeasure, String dtfrom, String dtto, com.itextpdf.text.Document doc) throws Exception {
        String paragraph = String.format("Measures for %1$s(%2$s) from end device with identifier %3$s \n from %4$s until %5$s",
                observableMeasure.getObservableProperty(), observableMeasure.getUnit(), observableMeasure.getIdentifier(), dtfrom, dtto);
        doc.add(new Paragraph(paragraph));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100.0f);
        table.setWidths(new float[]{3.0f, 2.0f, 2.0f,});
        table.setSpacingBefore(10);

        // define font for table header row
        Font font = FontFactory.getFont(FontFactory.HELVETICA);
        font.setColor(BaseColor.DARK_GRAY);

        // define table header cell
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(BaseColor.GREEN);
        cell.setPadding(5);

        // write table header
        cell.setPhrase(new Phrase("Measure date & time", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Measure value", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Measure unit", font));
        table.addCell(cell);

        BigDecimal valuemin = new BigDecimal(200.0);
        ;
        BigDecimal valuemax = new BigDecimal(-100.0);
        BigDecimal valuesum = new BigDecimal(0.0);

        // write table row data
        for (ValueTime valueTime : observableMeasure.getMeasuredata()) {
            valuesum = valuesum.add(valueTime.getValue());
            valuemax = valueTime.getValue().max(valuemax);
            valuemin = valueTime.getValue().min(valuemin);

            table.addCell(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(valueTime.getPhenomenonDateTime()));
            table.addCell(valueTime.getValue().toString());
            table.addCell(observableMeasure.getUnit());
        }
        BigDecimal avg = valuesum.divide(BigDecimal.valueOf(observableMeasure.getMeasuredata().size()), 2, BigDecimal.ROUND_CEILING);

        doc.add(table);
        doc.add(new Paragraph(
                String.format("\n minimum %1$s: %3$s %2$s \n maximum %1$s: %4$s %2$s \n average %1$s: %5$s %2$s",
                        observableMeasure.getObservableProperty(), observableMeasure.getUnit(),
                        valuemin.toString(), valuemax.toString(), avg.toString()
                )
        ));
        //new DecimalFormat("#0.##").format(avg)


        return doc;
    }

    private HSSFWorkbook createXlsx(ObservableMeasure observableMeasure) throws Exception {
        HSSFWorkbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet(observableMeasure.getObservableProperty());

        Row row = null;
        Cell cell = null;
        int r = 0;
        int c = 0;

        //Style for header cell
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.SEA_GREEN.index);
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setAlignment(CellStyle.ALIGN_CENTER);

        //Create header cells
        row = sheet.createRow(r++);

        cell = row.createCell(c++);
        cell.setCellStyle(style);
        cell.setCellValue("Measure date & time");

        cell = row.createCell(c++);
        cell.setCellStyle(style);
        cell.setCellValue("Measure value");

        cell = row.createCell(c++);
        cell.setCellStyle(style);
        cell.setCellValue("Measure unit");

        //Create data cell
        for (ValueTime valueTime : observableMeasure.getMeasuredata()) {
            row = sheet.createRow(r++);
            c = 0;
            row.createCell(c++).setCellValue(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(valueTime.getPhenomenonDateTime()));
            row.createCell(c++).setCellValue(valueTime.getValue().toString());
            row.createCell(c++).setCellValue(observableMeasure.getUnit());
        }

        for (int i = 0; i < observableMeasure.getMeasuredata().size(); i++)
            sheet.autoSizeColumn(i, true);

        return workbook;

    }

    private String convertTime(long time) {
        Date date = new Date(time);
        Format format = new SimpleDateFormat("yyyy MM dd HH:mm:ss");
        return format.format(date);
    }

}

