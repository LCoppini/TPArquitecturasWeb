import { Container } from '@/components/layout/Container'
import { RecommendedEvents } from '@/features/recommendations/components/RecommendedEvents'
import { CatalogHero } from '../components/CatalogHero'
import { EventCatalog } from '../components/EventCatalog'

export const HomePage = () => (
  <>
    <Container className="pt-4">
      <CatalogHero />
    </Container>
    <Container className="pt-12">
      <RecommendedEvents />
    </Container>
    <Container className="pt-16">
      <EventCatalog />
    </Container>
  </>
)
