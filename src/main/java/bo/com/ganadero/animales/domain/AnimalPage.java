package bo.com.ganadero.animales.domain; import java.util.List;
public record AnimalPage(List<Animal> content,int page,int size,long totalElements,int totalPages) {public static AnimalPage of(List<Animal> content,int page,int size,long total){return new AnimalPage(content,page,size,total,(int)Math.ceil((double)total/size));}}
