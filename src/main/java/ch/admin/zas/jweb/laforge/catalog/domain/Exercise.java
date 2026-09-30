package ch.admin.zas.jweb.laforge.catalog.domain;

import ch.admin.zas.jweb.laforge.common.persistence.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Identité stable d'un exercice, indépendante de ses versions successives. Ne porte aucun
 * contenu : toute donnée métier (énoncé, corrigé, indices...) vit sur {@link ExerciseVersion},
 * immuable une fois publiée.
 */
@Entity
@Table(name = "exercise")
public class Exercise extends BaseEntity {
}
