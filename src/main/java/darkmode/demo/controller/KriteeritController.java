package darkmode.demo.controller;

import darkmode.demo.dto.KriteeritDto;
import darkmode.demo.entity.Kriteerit;
import darkmode.demo.repository.KriteeritRepository;
import darkmode.demo.service.RecipeService;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class KriteeritController {

	private final KriteeritRepository kriteeritRepository;
	private final RecipeService recipeService;

	public KriteeritController(KriteeritRepository kriteeritRepository, RecipeService recipeService) {
		this.kriteeritRepository = kriteeritRepository;
		this.recipeService = recipeService;
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(Integer.class, new CustomNumberEditor(Integer.class, true));
	}

	@GetMapping("/")
	public String showForm(Model model) {
		model.addAttribute("kriteerit", new KriteeritDto());
		model.addAttribute("kriteeritList", kriteeritRepository.findAll());
		return "kriteerit";
	}

	@PostMapping("/post")
	public String saveKriteerit(@ModelAttribute("kriteerit") KriteeritDto kriteeritDto,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("kriteeritList", kriteeritRepository.findAll());
			return "kriteerit";
		}
		Kriteerit kriteerit = new Kriteerit(
				kriteeritDto.getKalorit(),
				kriteeritDto.getHiilihydraatit(),
				kriteeritDto.getProteiini(),
				kriteeritDto.getRasva(),
				kriteeritDto.getAnnoskoko());
		kriteeritRepository.save(kriteerit);
		return "redirect:/";
	}

	@PostMapping("/kriteerit/{id}/resepti")
	public String suggestRecipe(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		Kriteerit kriteerit = kriteeritRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		redirectAttributes.addFlashAttribute("recipe", recipeService.suggestRecipe(kriteerit));
		return "redirect:/";
	}

}
