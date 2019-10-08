//package com.ibm.common.core.file;
//package com.ibm.common.core.file;
//
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.lang.reflect.Field;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.List;
//
//import org.apache.poi.hssf.usermodel.HSSFWorkbook;
//import org.apache.poi.ss.usermodel.CellStyle;
//import org.apache.poi.ss.usermodel.Sheet;
//import org.apache.poi.ss.usermodel.Workbook;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Service;
//
//import com.ibm.common.utils.EmptyUtils;
//import com.ibm.common.utils.FileExtUtils;
//import com.ibm.common.utils.bean.BeanAnnotationUtils;
//import com.ibm.sc.camp.entity.report.ReportHeadData;
//import com.ibm.sc.dto.BaseDto;
//import com.sun.istack.internal.logging.Logger;
//import com.sun.rowset.internal.Row;
//
//import javafx.scene.control.Cell;
//
///**
// * POIXMLDoument office 相关文档服务 只支持 Excel文档
// * 
// * @author LiuBaoWen
// *
// */
//@Service
//public class POIXMLDocumentServiceImpl /*implements IPOIXMLDocumentService*/ {
//
//	private Logger logger = LoggerFactory.getLogger(POIXMLDocumentServiceImpl.class);
//
//	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//
//	// 每个sheet的数据量
//	private static final int sheetDataSize = 20000;
//
//	FileOutputStream fos = null;
//	InputStream is = null;
//	Workbook workbook = null;
//	String localFilePath = null;
//
//	/**
//	 * 获得Workbook对象
//	 * 
//	 * @author LiuBaoWen
//	 * @param templeteFile
//	 * @return
//	 */
//	private Workbook createWorkbook(String templeteFile) {
//		// 获取模板文件后缀
//		String suffix = FileExtUtils.getSuffix(templeteFile);
//		try {
//			is = this.getClass().getResourceAsStream(templeteFile);
//			if (suffix.equalsIgnoreCase("XLSX")) {
//				workbook = new XSSFWorkbook(is);
//			} else if (suffix.equalsIgnoreCase("XLS")) {
//				workbook = new HSSFWorkbook(is);
//			} else {
//				logger.error("模板文件:{} 不是Excel文件", templeteFile);
//				return null;
//			}
//		} catch (FileNotFoundException e1) {
//			logger.error("模板文件:{} 找不到, msg:{}, error:{}", templeteFile, e1.getMessage(), e1.getStackTrace());
//			return null;
//		} catch (IOException e1) {
//			logger.error("读取模板文件:{} 发生异常, msg:{}, error:{}", templeteFile, e1.getMessage(), e1.getStackTrace());
//			return null;
//		}
//		return workbook;
//	}
//
//	/**
//	 * 关闭Workbook对象
//	 * 
//	 * @author LiuBaoWen
//	 */
//	private void closeWorkbook() {
//		if (fos != null) {
//			try {
//				fos.close();
//			} catch (IOException e) {
//				logger.warn("关闭文件输出流发生异常,msg:{},error:{}", e.getMessage(), e.getStackTrace());
//			}
//		}
//		if (is != null) {
//			try {
//				is.close();
//			} catch (IOException e) {
//				logger.warn("关闭文件输入流发生异常,msg:{},error:{}", e.getMessage(), e.getStackTrace());
//			}
//		}
//		if (workbook != null) {
//			try {
//				workbook.close();
//			} catch (IOException e) {
//				logger.warn("关闭workbook发生异常,msg:{},error:{}", e.getMessage(), e.getStackTrace());
//			}
//		}
//	}
//
//	/**
//	 * 写入数据到Excel文档
//	 * 
//	 * @author LiuBaoWen
//	 * @param templeteFile
//	 * @param localFile
//	 * @param dataRow
//	 * @param dataList
//	 * @param clazz
//	 * @return
//	 * @throws Exception
//	 */
//	public <T extends BaseDto> boolean writeExcel(String templeteFile, String localFile, int dataRow, List<?> dataList,
//			List<ReportHeadData> headDataList, Class<?> clazz) throws Exception {
//		localFilePath = localFile;
//		// 处理单个报表开始时间
//		long startWriteTimeMillis = System.currentTimeMillis();
//		logger.info("写入数据到：{},开始时间：{}", localFilePath, new Date());
//		// 新建报表空文件
//		try {
//			FileExtUtils.createPathAndFile(localFilePath);
//			fos = new FileOutputStream(localFilePath);
//		} catch (IOException e2) {
//			logger.error("新建报表空文件：{}失败! msg:{},error:{}", localFilePath, e2.getMessage(), e2.getStackTrace());
//			return false;
//		}
//		// 创建Workbook对象
//		if (createWorkbook(templeteFile) == null) {
//			logger.error("获取workbook：{}失败!", localFilePath);
//			return false;
//		}
//		// 准备报表头数据
//		this.createReportHead(headDataList);
//		// 准备报表标题信息 test
//		// this.createReportTitle(dataRow,
//		// BeanAnnotationUtils.getOrderedBeanField(clazz.getDeclaredFields()));
//		// 准备报表数据
//		if (EmptyUtils.isNotEmpty(dataList)) {
//			// 分sheet创建报表数据
//			logger.info("拆分多个sheet生成报表");
//			this.createReportDataBySheet(dataRow, dataList,
//					BeanAnnotationUtils.getOrderedBeanField(clazz.getDeclaredFields()));
//			dataList = null;
//		}
//		// 写入报表excel
//		if (!this.writeWorkbook()) {
//			return false;
//		}
//		// 处理单个报表结束时间
//		long endWriteTimeMillis = System.currentTimeMillis();
//		logger.info("写入数据到：{},结束时间：{},耗时:{} 秒", localFilePath, new Date(),
//				(endWriteTimeMillis - startWriteTimeMillis) / 1000);
//		return true;
//	}
//
//	/**
//	 * 将Workbook写入Excel文件
//	 * 
//	 * @author LiuBaoWen
//	 * @return
//	 */
//	private boolean writeWorkbook() {
//		try {
//			// 写入数据
//			workbook.write(fos);
//		} catch (IOException e) {
//			logger.error("写入数据到报表文件:{} 发生异常,msg:{},error:{}", localFilePath, e.getMessage(), e.getStackTrace());
//			return false;
//		} finally {
//			this.closeWorkbook();
//		}
//		return true;
//	}
//
//	/**
//	 * 分sheet创建报表数据
//	 * 
//	 * @author LiuBaoWen
//	 * @param dataRowz
//	 * @param dataList
//	 * @param orderedBeanFields
//	 */
//	private <T> void createReportDataBySheet(int dataRow, List<T> dataList, List<Field> orderedBeanFields) {
//		dataRow--;
//		// 写入报表数据据
//		int dataListSize = dataList.size();
//		// sheet数量
//		int sheetCount = dataListSize / sheetDataSize;
//		// 最后一个sheet数据量
//		int lastSheetCount = dataListSize % sheetDataSize;
//		// sheet数量 （加上最后一个sheet）
//		sheetCount = lastSheetCount > 0 ? sheetCount + 1 : sheetCount;
//		// bean Field 数量
//		int orderedBeanFieldsSize = orderedBeanFields.size();
//		// 复制（sheetCount-1）个sheet
//		for (int sheetAtNum = 1; sheetAtNum < sheetCount; sheetAtNum++) {
//			workbook.cloneSheet(0);
//		}
//		try {
//			for (int sheetAtNum = 0; sheetAtNum < sheetCount; sheetAtNum++) {
//				// sheet数据起始index
//				int sheetAtDataFirstIndex = sheetDataSize * sheetAtNum;
//				// sheet数据结束index
//				int sheetAtDataLastIndex = sheetCount != (sheetAtNum + 1) ? sheetDataSize * (sheetAtNum + 1)
//						: dataListSize;
//
//				readDataToSheetAt(dataList, orderedBeanFields, dataRow, orderedBeanFieldsSize, sheetAtNum,
//						sheetAtDataFirstIndex, sheetAtDataLastIndex);
//			}
//		} catch (IllegalAccessException e) {
//			logger.error("数据与class不匹配,msg:{},error:{}", e.getMessage(), e.getStackTrace());
//			return;
//		} catch (Exception e) {
//			logger.error("写入报表数据异常,msg:{},error:{}", e.getMessage(), e.getStackTrace());
//			return;
//		}
//	}
//
//	/**
//	 * 创建报表数据
//	 * 
//	 * @author LiuBaoWen
//	 * @param dataRow
//	 * @param dataList
//	 * @param orderedBeanFields
//	 */
//	@SuppressWarnings("unused")
//	private <T> void createReportData(int dataRow, List<T> dataList, List<Field> orderedBeanFields) {
//		dataRow--;
//		Sheet sheetAt = workbook.getSheetAt(0);
//		// 写入报表数据
//		int dataListSize = dataList.size();
//		int orderedBeanFieldsSize = orderedBeanFields.size();
//		Row row = null;
//		try {
//			readDataToSheetAt(dataList, orderedBeanFields, dataRow, orderedBeanFieldsSize, 0, 0, dataListSize);
//		} catch (IllegalAccessException e) {
//			logger.error("数据与class不匹配,msg:{},error:{}", e.getMessage(), e.getStackTrace());
//			return;
//		} catch (Exception e) {
//			logger.error("写入报表数据异常,msg:{},error:{}", e.getMessage(), e.getStackTrace());
//			return;
//		}
//	}
//
//	/**
//	 * 读取数据到指定sheet
//	 * 
//	 * @author LiuBaoWen
//	 * @param dataList
//	 * @param orderedBeanFields
//	 * @param dataRow
//	 * @param orderedBeanFieldsSize
//	 * @param sheetAtNum
//	 * @param sheetAtDataFirstIndex
//	 * @param sheetAtDataLastIndex
//	 * @throws IllegalAccessException
//	 */
//	private <T> void readDataToSheetAt(List<T> dataList, List<Field> orderedBeanFields, int dataRow,
//			int orderedBeanFieldsSize, int sheetAtNum, int sheetAtDataFirstIndex, int sheetAtDataLastIndex)
//			throws IllegalAccessException {
//		Sheet sheetAt = workbook.getSheetAt(sheetAtNum);
//		Row row;
//		for (int i = sheetAtDataFirstIndex; i < sheetAtDataLastIndex; i++) {
//			T instance = dataList.get(i);
//			row = sheetAt.createRow(dataRow++);
//			// 数据的起始列
//			int dataColumn = 0;
//			for (int j = 0; j < orderedBeanFieldsSize; j++, dataColumn++) {
//				Field field = orderedBeanFields.get(j);
//				String fieldTypeName = field.getGenericType().getTypeName();
//				Object fieldValue = field.get(instance);
//				if (fieldValue != null) {
//					Cell cell = row.createCell(dataColumn);
//					switch (fieldTypeName) {// content
//					case "java.lang.String":
//						cell.setCellValue((String) fieldValue);
//						break;
//					case "int":
//						double d2 = Double.parseDouble(String.valueOf(fieldValue));
//						cell.setCellValue(d2);
//						break;
//					case "long":
//						double d = Double.valueOf(fieldValue.toString());
//						cell.setCellValue(d);
//						break;
//					case "float":
//						CellStyle floatStyle = workbook.createCellStyle();
//						short format = workbook.createDataFormat().getFormat(".00");// 保留2位精度
//						floatStyle.setDataFormat(format);
//						double d1 = Double.parseDouble(String.valueOf(fieldValue));
//						cell.setCellStyle(floatStyle);
//						cell.setCellValue(d1);
//						break;
//					case "double":
//						cell.setCellValue((double) fieldValue);
//						break;
//					case "java.util.Date":
//						/*
//						 * CellStyle dateStyle = workbook.createCellStyle(); short df =
//						 * workbook.createDataFormat().getFormat("yyyy-mm-dd");
//						 * dateStyle.setDataFormat(df); cell.setCellStyle(dateStyle); String format2 =
//						 * sdf.format(fieldValue); Date date = null; try { date = sdf.parse(format2);
//						 * cell.setCellValue(date); } catch (ParseException e) {
//						 * logger.warn("fieldTypeName:{},日期格式化异常,msg:{},error:{}", fieldTypeName,
//						 * e.getMessage(), e.getStackTrace()); }
//						 */
//						cell.setCellValue((Date) fieldValue);
//						break;
//					case "java.math.BigDecimal":
//						cell.setCellValue(fieldValue.toString());
//						break;
//					case "java.sql.Timestamp":
//						cell.setCellValue(fieldValue.toString());
//						break;
//					default:
//						logger.warn("columnName:{},不支持的数据类型:{}", field.getName(), fieldTypeName);
//					}
//				}
//			}
//		}
//	}
//
//	/**
//	 * 写入报表头数据
//	 * 
//	 * @author LiuBaoWen
//	 * @param sheetAt
//	 * @param ReportHeadDataList
//	 */
//	private void createReportHead(List<ReportHeadData> ReportHeadDataList) {
//		// 非空校验
//		if (EmptyUtils.isNull(ReportHeadDataList)) {
//			logger.info("报表头信息为空");
//			return;
//		}
//		Sheet sheetAt = workbook.getSheetAt(0);
//		for (ReportHeadData reportHeadData : ReportHeadDataList) {
//			// 行
//			int row = reportHeadData.getRow();
//			// 单元格
//			int cell = reportHeadData.getCell();
//			// 值
//			String value = reportHeadData.getValue();
//			// 给sheet单元格赋值
//			Row createRow = sheetAt.getRow(row - 1);
//			createRow.getCell(cell - 1).setCellValue(value);
//		}
//	}
//
//	/**
//	 * 生成报表标题数据 测试使用
//	 * 
//	 * @author LiuBaoWen
//	 * @param dataRow
//	 * @param declaredFields
//	 */
//	@SuppressWarnings("unused")
//	private void createReportTitle(int dataRow, List<Field> orderedBeanFields) {
//		Sheet sheetAt = workbook.getSheetAt(0);
//		// 写入报表标题信息
//		Row row = sheetAt.createRow(dataRow - 1);
//		int titleColumn = 0;// title的起始列
//		int orderedBeanFieldsSize = orderedBeanFields.size();
//		for (int m = 0; m < orderedBeanFieldsSize; m++) { // 设置title
//			Field field = orderedBeanFields.get(m);
//			String name = field.getName();
//			Cell cell = row.createCell(titleColumn++);
//			cell.setCellType(Cell.CELL_TYPE_STRING);
//			cell.setCellValue(name);
//		}
//	}
//
//}
