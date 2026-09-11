package bo.com.ganadero.potreros.domain; import java.util.List;
public record PotreroPage(List<Potrero> content,int page,int size,long totalElements,int totalPages) {public static PotreroPage of(List<Potrero> content,int page,int size,long total){return new PotreroPage(content,page,size,total,(int)Math.ceil((double)total/size));}}
