/**
 * Dominio: entidades, enumeraciones de estado, invariantes y políticas del
 * préstamo (State, Chain of Responsibility, Strategy). Las entidades usan
 * anotaciones {@code jakarta.persistence} para su mapeo (decisión DM-02 de
 * docs/agent-context/modelo-datos.md), pero no dependen de Spring ni de la capa
 * web. Los controladores nunca exponen estas entidades: usan DTO.
 */
package co.edu.itm.prestamosdeportivos.dominio;
