import { Chip } from '@/components/ui/Chip'
import { EVENT_CATEGORIES } from '../constants/categories'

type CategoryFilterProps = {
  /** Slug de la categoría elegida, o undefined si se muestran todas. */
  selected?: string
  onSelect: (slug: string | null) => void
}

export const CategoryFilter = ({ selected, onSelect }: CategoryFilterProps) => (
  <div role="group" aria-label="Categoría" className="flex flex-wrap gap-2">
    <Chip pressed={!selected} onClick={() => onSelect(null)}>
      Todos
    </Chip>
    {EVENT_CATEGORIES.map(({ slug, label }) => (
      <Chip key={slug} pressed={selected === slug} onClick={() => onSelect(slug)}>
        {label}
      </Chip>
    ))}
  </div>
)
