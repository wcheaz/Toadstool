// Production build (ng build). CloudFront routes /api/* to the Spring Boot API on EC2.
export const environment = {
  apiUrl: 'https://d5okymhrnlu0v.cloudfront.net/api'
};
