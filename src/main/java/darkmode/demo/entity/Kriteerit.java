package darkmode.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "kriteerit")
public class Kriteerit {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Integer kalorit;
	private Integer hiilihydraatit;
	private Integer proteiini;
	private Integer rasva;
	private Integer annoskoko;

	public Kriteerit() {
	}

	public Kriteerit(Integer kalorit, Integer hiilihydraatit, Integer proteiini, Integer rasva, Integer annoskoko) {
		this.kalorit = kalorit;
		this.hiilihydraatit = hiilihydraatit;
		this.proteiini = proteiini;
		this.rasva = rasva;
		this.annoskoko = annoskoko;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getKalorit() {
		return kalorit;
	}

	public void setKalorit(Integer kalorit) {
		this.kalorit = kalorit;
	}

	public Integer getHiilihydraatit() {
		return hiilihydraatit;
	}

	public void setHiilihydraatit(Integer hiilihydraatit) {
		this.hiilihydraatit = hiilihydraatit;
	}

	public Integer getProteiini() {
		return proteiini;
	}

	public void setProteiini(Integer proteiini) {
		this.proteiini = proteiini;
	}

	public Integer getRasva() {
		return rasva;
	}

	public void setRasva(Integer rasva) {
		this.rasva = rasva;
	}

	public Integer getAnnoskoko() {
		return annoskoko;
	}

	public void setAnnoskoko(Integer annoskoko) {
		this.annoskoko = annoskoko;
	}

}
