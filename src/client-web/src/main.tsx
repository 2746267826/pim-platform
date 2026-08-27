import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider, QueryCache, MutationCache } from '@tanstack/react-query'
import { Toaster } from 'sonner'
import App from './App'
import './index.css'
import { installInteractionDeferral } from './lib/autoRefresh'
import { showApiError } from './components/error/ApiErrorToast'

const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: (error) => showApiError(error),
  }),
  mutationCache: new MutationCache({
    onError: (error) => showApiError(error),
  }),
  defaultOptions: {
    queries: {
      retry: 2,
      retryDelay: 1000,
      refetchIntervalInBackground: false,
      structuralSharing: true,
    },
  },
})

installInteractionDeferral()

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
      <Toaster position="top-right" richColors closeButton toastOptions={{ className: 'pim-toast' }} />
    </QueryClientProvider>
  </React.StrictMode>
)
