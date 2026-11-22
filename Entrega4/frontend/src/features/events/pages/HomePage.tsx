import { Container } from '@/components/layout/Container'
import { CatalogHero } from '../components/CatalogHero'
import { EventCatalog } from '../components/EventCatalog'

export const HomePage = () => (
  <>
    <Container className="pt-4">
      <CatalogHero />
    </Container>
    <Container className="pt-16">
      <EventCatalog />
    </Container>
  </>
)
