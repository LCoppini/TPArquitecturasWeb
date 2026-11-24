import { Container } from '@/components/layout/Container'
import { Skeleton } from '@/components/ui/Skeleton'

export const EventDetailSkeleton = () => (
  <Container aria-busy="true" className="grid grid-cols-[1fr_420px] gap-12 pt-4">
    <div className="flex flex-col gap-6">
      <Skeleton className="h-64 rounded-xl" />
      <Skeleton className="h-4 w-24" />
      <Skeleton className="h-10 w-3/4" />
      <Skeleton className="h-5 w-1/2" />
    </div>
    <Skeleton className="h-96 rounded-xl" />
  </Container>
)
